using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using Microsoft.EntityFrameworkCore;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class CheckoutRepository(ApplicationDbContext dbContext) : ICheckoutRepository
{
    public async Task<CheckoutAttempt?> GetByOrderAndIdempotencyKeyAsync(
        long orderId,
        string idempotencyKey,
        CancellationToken cancellationToken)
    {
        return await dbContext.CheckoutAttempts
            .Include(attempt => attempt.PaymentTransaction)
            .Include(attempt => attempt.OutboxMessages)
            .SingleOrDefaultAsync(
                attempt => attempt.OrderId == orderId && attempt.IdempotencyKey == idempotencyKey,
                cancellationToken);
    }

    public async Task<CheckoutAttempt?> GetByIdAsync(long checkoutId, CancellationToken cancellationToken)
    {
        return await dbContext.CheckoutAttempts
            .AsNoTracking()
            .Include(attempt => attempt.PaymentTransaction)
            .Include(attempt => attempt.OutboxMessages)
            .SingleOrDefaultAsync(attempt => attempt.Id == checkoutId, cancellationToken);
    }

    public void AddAttempt(CheckoutAttempt attempt)
    {
        dbContext.CheckoutAttempts.Add(attempt);
    }

    public void AddPaymentTransaction(PaymentTransaction paymentTransaction)
    {
        dbContext.PaymentTransactions.Add(paymentTransaction);
    }

    public void AddInvoice(Invoice invoice)
    {
        dbContext.Invoices.Add(invoice);
    }

    public void AddOutboxMessages(IEnumerable<OutboxMessage> messages)
    {
        dbContext.OutboxMessages.AddRange(messages);
    }
}
