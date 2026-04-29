namespace ManagementPlatform.Domain;

public sealed class Tenant
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; }

    public List<Order> Orders { get; set; } = [];
}
