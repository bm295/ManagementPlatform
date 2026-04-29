using System.Collections.Concurrent;
using ManagementPlatform.Application;
using Microsoft.Extensions.Options;

namespace ManagementPlatform.Infrastructure;

public sealed class MockPaymentGateway(IOptions<MockIntegrationOptions> options) : IPaymentGateway
{
    private static readonly ConcurrentDictionary<long, int> Attempts = new();

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
            ? new PaymentGatewayResult(true, $"pay_{request.CheckoutAttemptId}", null)
            : new PaymentGatewayResult(false, null, settings.PaymentFailureReason);

        return Task.FromResult(result);
    }
}
