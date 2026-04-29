namespace ManagementPlatform.Domain;

public sealed class Invoice
{
    public long Id { get; set; }
    public long CheckoutAttemptId { get; set; }
    public CheckoutAttempt CheckoutAttempt { get; set; } = null!;
    public InvoiceStatus Status { get; set; } = InvoiceStatus.Pending;
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? CompletedAt { get; set; }
}
