using ManagementPlatform.Application;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using System.Collections.Concurrent;

namespace ManagementPlatform.Infrastructure;

public sealed class MockIntegrationOptions
{
    public bool PaymentSucceeds { get; set; } = true;
    public bool EmailSucceeds { get; set; } = true;
    public bool InvoiceSucceeds { get; set; } = true;
    public bool ProductionSucceeds { get; set; } = true;
    public int PaymentRetrySuccessAttempt { get; set; } = 3;
    public string PaymentFailureReason { get; set; } = "Payment was declined by the mock provider.";
}

public sealed class MockPaymentGateway(IOptions<MockIntegrationOptions> options) : IPaymentGateway
{
    private static readonly ConcurrentDictionary<Guid, int> Attempts = new();

    public Task<PaymentGatewayResult> ChargeAsync(
        PaymentGatewayRequest request,
        CancellationToken cancellationToken)
    {
        var settings = options.Value;
        var attemptNumber = Attempts.AddOrUpdate(request.CheckoutAttemptId, 1, (_, current) => current + 1);
        var requestedFailure = request.PaymentMethodToken.Contains("fail", StringComparison.OrdinalIgnoreCase);
        var requestedRetryFailure = request.PaymentMethodToken.Contains("retry-fail", StringComparison.OrdinalIgnoreCase);
        var requestedRetrySuccess = request.PaymentMethodToken.Contains("retry-success", StringComparison.OrdinalIgnoreCase);

        if (requestedRetrySuccess && attemptNumber < Math.Max(settings.PaymentRetrySuccessAttempt, 1))
        {
            return Task.FromResult(new PaymentGatewayResult(
                false,
                null,
                "Temporary payment gateway error.",
                true));
        }

        if (requestedRetryFailure)
        {
            return Task.FromResult(new PaymentGatewayResult(
                false,
                null,
                "Temporary payment gateway error.",
                true));
        }

        var result = settings.PaymentSucceeds && !requestedFailure
            ? new PaymentGatewayResult(true, $"pay_{request.CheckoutAttemptId:N}", null)
            : new PaymentGatewayResult(false, null, settings.PaymentFailureReason);

        return Task.FromResult(result);
    }
}

public sealed class MockEmailSender(
    IOptions<MockIntegrationOptions> options,
    ILogger<MockEmailSender> logger) : IEmailSender
{
    public Task SendCheckoutSucceededAsync(
        CheckoutEmailPayload payload,
        CancellationToken cancellationToken)
    {
        if (!options.Value.EmailSucceeds)
        {
            throw new InvalidOperationException("Mock email service failed.");
        }

        logger.LogInformation(
            "Checkout email queued for {Email} and order {OrderId}.",
            payload.CustomerEmail,
            payload.OrderId);

        return Task.CompletedTask;
    }
}

public sealed class MockInvoiceClient(IOptions<MockIntegrationOptions> options) : IInvoiceClient
{
    public Task<InvoiceClientResult> CreateInvoiceAsync(
        InvoicePayload payload,
        CancellationToken cancellationToken)
    {
        if (!options.Value.InvoiceSucceeds)
        {
            throw new InvalidOperationException("Mock invoice service failed.");
        }

        return Task.FromResult(new InvoiceClientResult($"inv_{payload.CheckoutAttemptId:N}"));
    }
}

public sealed class MockProductionClient(
    IOptions<MockIntegrationOptions> options,
    ILogger<MockProductionClient> logger) : IProductionClient
{
    public Task PushOrderAsync(
        ProductionOrderPayload payload,
        CancellationToken cancellationToken)
    {
        if (!options.Value.ProductionSucceeds)
        {
            throw new InvalidOperationException("Mock production service failed.");
        }

        logger.LogInformation(
            "Order {OrderId} pushed to the production service.",
            payload.OrderId);

        return Task.CompletedTask;
    }
}
