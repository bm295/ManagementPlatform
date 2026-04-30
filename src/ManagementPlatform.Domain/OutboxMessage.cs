namespace ManagementPlatform.Domain;

public sealed class OutboxMessage
{
    public long Id { get; set; }
    public long CheckoutAttemptId { get; set; }
    public CheckoutAttempt CheckoutAttempt { get; set; } = null!;
    public OutboxMessageType Type { get; set; }
    public OutboxStatus Status { get; set; } = OutboxStatus.Pending;
    public string PayloadJson { get; set; } = "{}";
    public int AttemptCount { get; set; }
    public string? LastError { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? NextAttemptAt { get; set; }
    public DateTimeOffset? LockedAt { get; set; }
    public DateTimeOffset? ProcessedAt { get; set; }
}
