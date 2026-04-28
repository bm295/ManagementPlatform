namespace ManagementPlatform.Infrastructure.Outbox;

public sealed class OutboxOptions
{
    public bool Enabled { get; set; } = true;
    public int BatchSize { get; set; } = 25;
    public int MaxAttempts { get; set; } = 5;
    public TimeSpan PollInterval { get; set; } = TimeSpan.FromSeconds(5);
}
