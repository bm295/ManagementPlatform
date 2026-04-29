using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed record CheckoutRequest(
    string IdempotencyKey,
    string PaymentMethodToken);

public sealed record CheckoutResponse(
    long CheckoutId,
    long OrderId,
    CheckoutStatus Status,
    PaymentStatus? PaymentStatus,
    string? FailureReason,
    IReadOnlyList<IntegrationStatusDto> Integrations);

public sealed record IntegrationStatusDto(
    OutboxMessageType Type,
    OutboxStatus Status,
    int Attempts,
    string? LastError);
