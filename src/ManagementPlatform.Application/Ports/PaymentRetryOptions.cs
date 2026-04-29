namespace ManagementPlatform.Application;

public sealed class PaymentRetryOptions
{
    public int MaxAttempts { get; set; } = 3;
    public int DelayMilliseconds { get; set; } = 200;
}
