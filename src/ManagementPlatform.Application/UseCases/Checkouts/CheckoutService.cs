using System.Text.Json;
using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed class CheckoutService(
    IOrderRepository orderRepository,
    ICheckoutRepository checkoutRepository,
    IUnitOfWork unitOfWork,
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
        if (string.IsNullOrWhiteSpace(request.IdempotencyKey))
        {
            throw new ValidationException("An idempotency key is required.");
        }

        if (string.IsNullOrWhiteSpace(request.PaymentMethodToken))
        {
            throw new ValidationException("A payment method token is required.");
        }

        var idempotencyKey = request.IdempotencyKey.Trim();
        var existingAttempt = await checkoutRepository.GetByOrderAndIdempotencyKeyAsync(
            orderId,
            idempotencyKey,
            cancellationToken);

        if (existingAttempt is not null)
        {
            return ToResponse(existingAttempt);
        }

        var order = await orderRepository.GetForCheckoutAsync(orderId, cancellationToken);

        if (order is null)
        {
            throw new NotFoundException("Order was not found.");
        }

        if (order.Status is not OrderStatus.Draft)
        {
            throw new ConflictException("Order is already paid or being processed.");
        }

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
        await unitOfWork.SaveChangesAsync(cancellationToken);

        var paymentResult = await ChargeWithRetryAsync(order, attempt, request.PaymentMethodToken, cancellationToken);

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

        if (!paymentResult.Succeeded)
        {
            attempt.Status = CheckoutStatus.PaymentFailed;
            attempt.FailureReason = paymentResult.FailureReason ?? "Payment was declined.";
            attempt.CompletedAt = clock.UtcNow;
            order.Status = OrderStatus.Draft;
            await unitOfWork.SaveChangesAsync(cancellationToken);

            return ToResponse(attempt);
        }

        attempt.Status = CheckoutStatus.PaymentSucceeded;
        attempt.CompletedAt = clock.UtcNow;
        order.Status = OrderStatus.Paid;
        order.PaidAt = clock.UtcNow;

        var invoice = new Invoice
        {
            CheckoutAttemptId = attempt.Id,
            Status = InvoiceStatus.Pending,
            CreatedAt = clock.UtcNow
        };

        attempt.Invoice = invoice;
        checkoutRepository.AddInvoice(invoice);

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

        await unitOfWork.SaveChangesAsync(cancellationToken);
        return ToResponse(attempt);
    }

    public async Task<CheckoutResponse> GetAsync(long checkoutId, CancellationToken cancellationToken)
    {
        var attempt = await checkoutRepository.GetByIdAsync(checkoutId, cancellationToken);

        return attempt is null
            ? throw new NotFoundException("Checkout was not found.")
            : ToResponse(attempt);
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
                message.Attempts,
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
