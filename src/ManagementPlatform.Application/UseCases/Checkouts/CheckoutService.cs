using System.Text.Json;
using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed class CheckoutService(
    IOrderRepository orderRepository,
    ICheckoutRepository checkoutRepository,
    IAppDbSession appDbSession,
    IPaymentGateway paymentGateway,
    IClock clock,
    PaymentRetryOptions paymentRetryOptions)
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    public async Task<CheckoutResponse> CheckoutAsync(
        long orderId,
        CheckoutRequest request,
        CancellationToken cancellationToken)
    {
        var idempotencyKey = ValidateAndNormalizeRequest(request);

        var existingAttempt = await checkoutRepository.GetByOrderAndIdempotencyKeyAsync(
            orderId,
            idempotencyKey,
            cancellationToken);

        if (existingAttempt is not null)
        {
            return ToResponse(existingAttempt);
        }

        var order = await LoadValidOrderAsync(orderId, cancellationToken);
        var attempt = await CreatePendingAttemptAsync(order, idempotencyKey, cancellationToken);

        var paymentResult = await ChargeWithRetryAsync(order, attempt, request.PaymentMethodToken, cancellationToken);
        CreateAndAttachPaymentTransaction(order, attempt, paymentResult);

        if (!paymentResult.Succeeded)
        {
            await HandlePaymentFailureAsync(order, attempt, paymentResult, cancellationToken);
            return ToResponse(attempt);
        }

        MarkPaymentSuccess(order, attempt);
        CreateAndAttachInvoice(attempt);
        CreateAndAttachOutboxMessages(order, attempt);

        await appDbSession.SaveChangesAsync(cancellationToken);
        return ToResponse(attempt);
    }

    public async Task<CheckoutResponse> GetAsync(long checkoutId, CancellationToken cancellationToken)
    {
        var attempt = await checkoutRepository.GetByIdAsync(checkoutId, cancellationToken);

        return attempt is null
            ? throw new NotFoundException("Checkout was not found.")
            : ToResponse(attempt);
    }

    private static string ValidateAndNormalizeRequest(CheckoutRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.IdempotencyKey))
        {
            throw new ValidationException("An idempotency key is required.");
        }

        if (string.IsNullOrWhiteSpace(request.PaymentMethodToken))
        {
            throw new ValidationException("A payment method token is required.");
        }

        return request.IdempotencyKey.Trim();
    }

    private async Task<Order> LoadValidOrderAsync(long orderId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetForCheckoutAsync(orderId, cancellationToken);

        if (order is null)
        {
            throw new NotFoundException("Order was not found.");
        }

        if (order.Status is not OrderStatus.Draft)
        {
            throw new ConflictException("Order is already paid or being processed.");
        }

        return order;
    }

    private async Task<CheckoutAttempt> CreatePendingAttemptAsync(
        Order order,
        string idempotencyKey,
        CancellationToken cancellationToken)
    {
        var now = clock.UtcNow;
        order.Status = OrderStatus.CheckoutProcessing;

        var attempt = new CheckoutAttempt
        {
            OrderId = order.Id,
            IdempotencyKey = idempotencyKey,
            Status = CheckoutStatus.PaymentPending,
            CreatedAt = now
        };

        checkoutRepository.AddAttempt(attempt);
        await appDbSession.SaveChangesAsync(cancellationToken);

        return attempt;
    }

    private void CreateAndAttachPaymentTransaction(
        Order order,
        CheckoutAttempt attempt,
        RetriedPaymentResult paymentResult)
    {
        var paymentTransaction = new PaymentTransaction
        {
            CheckoutAttemptId = attempt.Id,
            Status = paymentResult.Succeeded ? PaymentStatus.Succeeded : PaymentStatus.Failed,
            AttemptCount = paymentResult.AttemptCount,
            Amount = order.Amount,
            Currency = order.Currency,
            ProviderTransactionId = paymentResult.ProviderTransactionId,
            FailureReason = paymentResult.FailureReason,
            CreatedAt = clock.UtcNow
        };

        attempt.PaymentTransaction = paymentTransaction;
        checkoutRepository.AddPaymentTransaction(paymentTransaction);
    }

    private async Task HandlePaymentFailureAsync(
        Order order,
        CheckoutAttempt attempt,
        RetriedPaymentResult paymentResult,
        CancellationToken cancellationToken)
    {
        attempt.Status = CheckoutStatus.PaymentFailed;
        attempt.FailureReason = paymentResult.FailureReason ?? "Payment was declined.";
        attempt.CompletedAt = clock.UtcNow;
        order.Status = OrderStatus.Draft;

        await appDbSession.SaveChangesAsync(cancellationToken);
    }

    private void MarkPaymentSuccess(Order order, CheckoutAttempt attempt)
    {
        attempt.Status = CheckoutStatus.PaymentSucceeded;
        attempt.CompletedAt = clock.UtcNow;
        order.Status = OrderStatus.Paid;
        order.PaidAt = clock.UtcNow;
    }

    private void CreateAndAttachInvoice(CheckoutAttempt attempt)
    {
        var invoice = new Invoice
        {
            CheckoutAttemptId = attempt.Id,
            Status = InvoiceStatus.Pending,
            CreatedAt = clock.UtcNow
        };

        attempt.Invoice = invoice;
        checkoutRepository.AddInvoice(invoice);
    }

    private void CreateAndAttachOutboxMessages(Order order, CheckoutAttempt attempt)
    {
        var emailPayload = new CheckoutEmailPayload(
            attempt.Id,
            order.Id,
            order.Name,
            order.Tenant.Email,
            order.Amount,
            order.Currency);

        var productionPayload = new ProductionOrderPayload(
            attempt.Id,
            order.Id,
            order.Name,
            order.Amount,
            order.Currency);

        var outboxMessages = new[]
        {
            CreateOutboxMessage(attempt.Id, OutboxMessageType.SendCheckoutEmail, emailPayload),
            CreateOutboxMessage(attempt.Id, OutboxMessageType.PushToProduction, productionPayload)
        };

        attempt.OutboxMessages.AddRange(outboxMessages);
        checkoutRepository.AddOutboxMessages(outboxMessages);
    }

    private async Task<RetriedPaymentResult> ChargeWithRetryAsync(
        Order order,
        CheckoutAttempt attempt,
        string paymentMethodToken,
        CancellationToken cancellationToken)
    {
        var maxAttempts = Math.Max(paymentRetryOptions.MaxAttempts, 1);
        PaymentGatewayResult? latestResult = null;

        for (var attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber++)
        {
            latestResult = await paymentGateway.ChargeAsync(
                new PaymentGatewayRequest(
                    order.Id,
                    attempt.Id,
                    order.Amount,
                    order.Currency,
                    paymentMethodToken),
                cancellationToken);

            if (latestResult.Succeeded)
            {
                return new RetriedPaymentResult(
                    true,
                    latestResult.ProviderTransactionId,
                    null,
                    attemptNumber);
            }

            if (!latestResult.IsRetryable || attemptNumber == maxAttempts)
            {
                return new RetriedPaymentResult(
                    false,
                    null,
                    latestResult.FailureReason,
                    attemptNumber);
            }

            var delay = Math.Max(paymentRetryOptions.DelayMilliseconds, 0);
            if (delay > 0)
            {
                await Task.Delay(delay, cancellationToken);
            }
        }

        return new RetriedPaymentResult(false, null, latestResult?.FailureReason, maxAttempts);
    }

    private OutboxMessage CreateOutboxMessage<TPayload>(
        long checkoutAttemptId,
        OutboxMessageType type,
        TPayload payload)
    {
        return new OutboxMessage
        {
            CheckoutAttemptId = checkoutAttemptId,
            Type = type,
            Status = OutboxStatus.Pending,
            PayloadJson = JsonSerializer.Serialize(payload, JsonOptions),
            CreatedAt = clock.UtcNow
        };
    }

    private static CheckoutResponse ToResponse(CheckoutAttempt attempt)
    {
        var integrations = attempt.OutboxMessages
            .OrderBy(message => message.Type)
            .Select(message => new IntegrationStatusDto(
                message.Type,
                message.Status,
                message.AttemptCount,
                message.LastError))
            .ToArray();

        return new CheckoutResponse(
            attempt.Id,
            attempt.OrderId,
            attempt.Status,
            attempt.PaymentTransaction?.Status,
            attempt.FailureReason ?? attempt.PaymentTransaction?.FailureReason,
            integrations);
    }

    private sealed record RetriedPaymentResult(
        bool Succeeded,
        string? ProviderTransactionId,
        string? FailureReason,
        int AttemptCount);
}
