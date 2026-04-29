using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public interface ICheckoutRepository
{
    Task<CheckoutAttempt?> GetByOrderAndIdempotencyKeyAsync(
        long orderId,
        string idempotencyKey,
        CancellationToken cancellationToken);

    Task<CheckoutAttempt?> GetByIdAsync(long checkoutId, CancellationToken cancellationToken);

    void AddAttempt(CheckoutAttempt attempt);
    void AddPaymentTransaction(PaymentTransaction paymentTransaction);
    void AddInvoice(Invoice invoice);
    void AddOutboxMessages(IEnumerable<OutboxMessage> messages);
}
