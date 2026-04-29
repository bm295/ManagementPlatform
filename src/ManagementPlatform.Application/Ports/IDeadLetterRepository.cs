namespace ManagementPlatform.Application;

public interface IDeadLetterRepository
{
    Task<IReadOnlyList<DeadLetterMessageDto>> GetRecentAsync(CancellationToken cancellationToken);
}
