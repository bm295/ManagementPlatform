using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed record DeadLetterMessageDto(
    long Id,
    long CheckoutAttemptId,
    long OutboxMessageId,
    OutboxMessageType Type,
    int AttemptCount,
    string FailureReason,
    DateTimeOffset FailedAt);
