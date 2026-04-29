namespace ManagementPlatform.Application;

public interface IClock
{
    DateTimeOffset UtcNow { get; }
}

public interface IPaymentGateway
{
    Task<PaymentGatewayResult> ChargeAsync(PaymentGatewayRequest request, CancellationToken cancellationToken);
}

public interface IEmailSender
{
    Task SendCheckoutSucceededAsync(CheckoutEmailPayload payload, CancellationToken cancellationToken);
}

public interface IProductionClient
{
    Task PushOrderAsync(ProductionOrderPayload payload, CancellationToken cancellationToken);
}
