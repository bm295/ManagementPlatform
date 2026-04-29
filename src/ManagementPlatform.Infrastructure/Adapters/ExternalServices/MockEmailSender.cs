using ManagementPlatform.Application;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace ManagementPlatform.Infrastructure;

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
            payload.TenantEmail,
            payload.OrderId);

        return Task.CompletedTask;
    }
}
