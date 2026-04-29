namespace ManagementPlatform.Application;

public sealed record PaymentGatewayRequest(
    long OrderId,
    long CheckoutAttemptId,
    decimal Amount,
    string Currency,
    string PaymentMethodToken);

public sealed record PaymentGatewayResult(
    bool Succeeded,
    string? ProviderTransactionId,
    string? FailureReason,
    bool IsRetryable = false);

public sealed record CheckoutEmailPayload(
    long CheckoutAttemptId,
    long OrderId,
    string OrderName,
    string TenantEmail,
    decimal Amount,
    string Currency);

public sealed record ProductionOrderPayload(
    long CheckoutAttemptId,
    long OrderId,
    string OrderName,
    decimal Amount,
    string Currency);
