namespace ManagementPlatform.Domain;

public sealed class PaymentTransaction
{
    public long Id { get; set; }
    public long CheckoutAttemptId { get; set; }
    public CheckoutAttempt CheckoutAttempt { get; set; } = null!;
    public PaymentStatus Status { get; set; }
    public int AttemptCount { get; set; }
    public decimal Amount { get; set; }
    public string Currency { get; set; } = "USD";
    public string? ProviderTransactionId { get; set; }
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
}
