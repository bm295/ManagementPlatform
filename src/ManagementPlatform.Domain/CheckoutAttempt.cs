namespace ManagementPlatform.Domain;

public sealed class CheckoutAttempt
{
    public long Id { get; set; }
    public long OrderId { get; set; }
    public Order Order { get; set; } = null!;
    public string IdempotencyKey { get; set; } = string.Empty;
    public CheckoutStatus Status { get; set; } = CheckoutStatus.PaymentPending;
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? CompletedAt { get; set; }

    public PaymentTransaction? PaymentTransaction { get; set; }
    public Invoice? Invoice { get; set; }
    public List<OutboxMessage> OutboxMessages { get; set; } = [];
}
