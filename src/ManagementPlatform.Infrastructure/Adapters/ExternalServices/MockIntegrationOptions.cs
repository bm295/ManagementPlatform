namespace ManagementPlatform.Infrastructure;

public sealed class MockIntegrationOptions
{
    public bool PaymentSucceeds { get; set; } = true;
    public bool EmailSucceeds { get; set; } = true;
    public bool ProductionSucceeds { get; set; } = true;
    public int PaymentRetrySuccessAttempt { get; set; } = 3;
    public string PaymentFailureReason { get; set; } = "Payment was declined by the mock provider.";
}
