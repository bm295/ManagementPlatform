using ManagementPlatform.Domain;
using Microsoft.EntityFrameworkCore;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class DbSeeder(ApplicationDbContext dbContext)
{
    public async Task SeedAsync(CancellationToken cancellationToken = default)
    {
        if (await dbContext.Customers.AnyAsync(cancellationToken))
        {
            return;
        }

        var now = DateTimeOffset.UtcNow;

        var studio = new Customer
        {
            Id = Guid.Parse("16d35851-4b88-4fd1-96d6-7627fc1248dc"),
            Name = "Northwind Studio",
            Email = "orders@northwind.example",
            CreatedAt = now
        };

        var agency = new Customer
        {
            Id = Guid.Parse("f9f375d6-7b13-4f78-8a04-bf483ec17c78"),
            Name = "Aperture Creative",
            Email = "billing@aperture.example",
            CreatedAt = now
        };

        dbContext.Customers.AddRange(studio, agency);
        dbContext.Orders.AddRange(
            new Order
            {
                Id = Guid.Parse("7cf2feda-02df-48fc-ae4a-a9f47a5f3c18"),
                CustomerId = studio.Id,
                Name = "Spring Catalog Retouch",
                Amount = 1260.00m,
                Currency = "USD",
                CreatedAt = now.AddDays(-5)
            },
            new Order
            {
                Id = Guid.Parse("c0faeae7-46dd-48f5-a831-1f717bc16f6b"),
                CustomerId = studio.Id,
                Name = "Holiday Product Set",
                Amount = 840.00m,
                Currency = "USD",
                CreatedAt = now.AddDays(-3)
            },
            new Order
            {
                Id = Guid.Parse("74cc9434-5eed-4587-b194-fb8572fa9827"),
                CustomerId = agency.Id,
                Name = "Marketplace Launch Batch",
                Amount = 2195.50m,
                Currency = "USD",
                CreatedAt = now.AddDays(-1)
            });

        await dbContext.SaveChangesAsync(cancellationToken);
    }
}
