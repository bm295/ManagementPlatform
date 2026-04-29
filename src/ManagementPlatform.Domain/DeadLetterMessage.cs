namespace ManagementPlatform.Domain;

public sealed class DeadLetterMessage
{
    public long Id { get; set; }
    public long OutboxMessageId { get; set; }
    public long CheckoutAttemptId { get; set; }
    public OutboxMessageType Type { get; set; }
    public string PayloadJson { get; set; } = "{}";
    public int AttemptCount { get; set; }
    public string FailureReason { get; set; } = string.Empty;
    public DateTimeOffset FailedAt { get; set; }
}
