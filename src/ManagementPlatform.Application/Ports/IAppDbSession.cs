namespace ManagementPlatform.Application;

public interface IAppDbSession
{
    Task<int> SaveChangesAsync(CancellationToken cancellationToken = default);
}
