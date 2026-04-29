using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed record OrderSummaryDto(
    long Id,
    string Name,
    string TenantName,
    decimal Amount,
    string Currency,
    OrderStatus Status,
    DateTimeOffset CreatedAt);

public sealed record OrderDetailsDto(
    long Id,
    string Name,
    string TenantName,
    string TenantEmail,
    decimal Amount,
    string Currency,
    OrderStatus Status,
    DateTimeOffset CreatedAt,
    DateTimeOffset? PaidAt);
