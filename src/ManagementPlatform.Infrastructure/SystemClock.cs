using ManagementPlatform.Application;

namespace ManagementPlatform.Infrastructure;

public sealed class SystemClock : IClock
{
    public DateTimeOffset UtcNow => DateTimeOffset.UtcNow;
}
