namespace ManagementPlatform.Domain;

public sealed class Order
{
    public long Id { get; set; }
    public long TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public string Name { get; set; } = string.Empty;
    public decimal Amount { get; set; }
    public string Currency { get; set; } = "USD";
    public OrderStatus Status { get; set; } = OrderStatus.Draft;
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? PaidAt { get; set; }

    public List<CheckoutAttempt> CheckoutAttempts { get; set; } = [];
}
