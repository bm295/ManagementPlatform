using ManagementPlatform.Application;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace ManagementPlatform.Infrastructure;

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
