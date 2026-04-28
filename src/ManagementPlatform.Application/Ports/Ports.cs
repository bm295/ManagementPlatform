using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public interface IUnitOfWork
{
    Task<int> SaveChangesAsync(CancellationToken cancellationToken = default);
}

public interface IOrderRepository
{
    Task<PagedResult<OrderSummaryDto>> SearchAsync(
        string? name,
        int page,
        int pageSize,
        CancellationToken cancellationToken);

    Task<OrderDetailsDto?> GetDetailsAsync(Guid orderId, CancellationToken cancellationToken);

    Task<Order?> GetForCheckoutAsync(Guid orderId, CancellationToken cancellationToken);
}

public interface ICheckoutRepository
{
    Task<CheckoutAttempt?> GetByOrderAndIdempotencyKeyAsync(
        Guid orderId,
        string idempotencyKey,
        CancellationToken cancellationToken);

    Task<CheckoutAttempt?> GetByIdAsync(Guid checkoutId, CancellationToken cancellationToken);

    void AddAttempt(CheckoutAttempt attempt);
    void AddPaymentTransaction(PaymentTransaction paymentTransaction);
    void AddInvoiceRequest(InvoiceRequest invoiceRequest);
    void AddOutboxMessages(IEnumerable<OutboxMessage> messages);
}

public interface IDeadLetterRepository
{
    Task<IReadOnlyList<DeadLetterMessageDto>> GetRecentAsync(CancellationToken cancellationToken);
}

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

public interface IInvoiceClient
{
    Task<InvoiceClientResult> CreateInvoiceAsync(InvoicePayload payload, CancellationToken cancellationToken);
}

public interface IProductionClient
{
    Task PushOrderAsync(ProductionOrderPayload payload, CancellationToken cancellationToken);
}

public sealed record PaymentGatewayRequest(
    Guid OrderId,
    Guid CheckoutAttemptId,
    decimal Amount,
    string Currency,
    string PaymentMethodToken);

public sealed record PaymentGatewayResult(
    bool Succeeded,
    string? ProviderTransactionId,
    string? FailureReason,
    bool IsRetryable = false);

public sealed record InvoiceClientResult(string ExternalInvoiceId);

public sealed record CheckoutEmailPayload(
    Guid CheckoutAttemptId,
    Guid OrderId,
    string OrderName,
    string CustomerEmail,
    decimal Amount,
    string Currency);

public sealed record InvoicePayload(
    Guid CheckoutAttemptId,
    Guid OrderId,
    string OrderName,
    string CustomerEmail,
    decimal Amount,
    string Currency);

public sealed record ProductionOrderPayload(
    Guid CheckoutAttemptId,
    Guid OrderId,
    string OrderName,
    Guid CustomerId,
    decimal Amount,
    string Currency);

public sealed class PaymentRetryOptions
{
    public int MaxAttempts { get; set; } = 3;
    public int DelayMilliseconds { get; set; } = 200;
}
