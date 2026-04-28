using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed record DeadLetterMessageDto(
    Guid Id,
    Guid CheckoutAttemptId,
    Guid OutboxMessageId,
    OutboxMessageType Type,
    int AttemptCount,
    string FailureReason,
    DateTimeOffset FailedAt);

public sealed class DeadLetterQueryService(IDeadLetterRepository deadLetterRepository)
{
    public Task<IReadOnlyList<DeadLetterMessageDto>> GetRecentAsync(CancellationToken cancellationToken)
    {
        return deadLetterRepository.GetRecentAsync(cancellationToken);
    }
}
