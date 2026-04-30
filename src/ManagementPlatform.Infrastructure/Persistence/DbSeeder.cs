using ManagementPlatform.Domain;
using Microsoft.EntityFrameworkCore;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class DbSeeder(ApplicationDbContext dbContext)
{
    public async Task SeedAsync(CancellationToken cancellationToken = default)
    {
        if (await dbContext.Tenants.AnyAsync(cancellationToken))
        {
            return;
        }

        var now = DateTimeOffset.UtcNow;
        var northwind = new Tenant
        {
            Name = "Northwind Studio",
            Email = "orders@northwind.example",
            CreatedAt = now
        };

        var aperture = new Tenant
        {
            Name = "Aperture Creative",
            Email = "billing@aperture.example",
            CreatedAt = now
        };

        dbContext.Tenants.AddRange(northwind, aperture);
        dbContext.Orders.AddRange(
            new Order
            {
                Tenant = northwind,
                Name = "Spring Catalog Retouch",
                Amount = 1260.00m,
                Currency = "USD",
                CreatedAt = now.AddDays(-5)
            },
            new Order
            {
                Tenant = northwind,
                Name = "Holiday Product Set",
                Amount = 840.00m,
                Currency = "USD",
                CreatedAt = now.AddDays(-3)
            },
            new Order
            {
                Tenant = aperture,
                Name = "Marketplace Launch Batch",
                Amount = 2195.50m,
                Currency = "USD",
                CreatedAt = now.AddDays(-1)
            },
            new Order
            {
                Tenant = aperture,
                Name = "Summer Campaign Variants",
                Amount = 1475.25m,
                Currency = "USD",
                CreatedAt = now
            });

        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
