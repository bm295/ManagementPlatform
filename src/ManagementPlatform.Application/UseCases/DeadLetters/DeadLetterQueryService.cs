namespace ManagementPlatform.Application;

public sealed class DeadLetterQueryService(IDeadLetterRepository deadLetterRepository)
{
    public Task<IReadOnlyList<DeadLetterMessageDto>> GetRecentAsync(CancellationToken cancellationToken)
    {
        return deadLetterRepository.GetRecentAsync(cancellationToken);
    }
}
