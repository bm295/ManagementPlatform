using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using Microsoft.EntityFrameworkCore;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class OrderRepository(ApplicationDbContext dbContext) : IOrderRepository
{
    public async Task<PagedResult<OrderSummaryDto>> SearchAsync(
        string? name,
        int page,
        int pageSize,
        CancellationToken cancellationToken)
    {
        var query = dbContext.Orders
            .AsNoTracking()
            .Include(order => order.Customer)
            .AsQueryable();

        if (!string.IsNullOrWhiteSpace(name))
        {
            var normalizedName = name.Trim().ToLower();
            query = query.Where(order => order.Name.ToLower().Contains(normalizedName));
        }

        var totalCount = await query.CountAsync(cancellationToken);
        var items = await query
            .OrderByDescending(order => order.CreatedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(order => new OrderSummaryDto(
                order.Id,
                order.Name,
                order.Customer.Name,
                order.Amount,
                order.Currency,
                order.Status,
                order.CreatedAt))
            .ToListAsync(cancellationToken);

        return new PagedResult<OrderSummaryDto>(items, page, pageSize, totalCount);
    }

    public async Task<OrderDetailsDto?> GetDetailsAsync(Guid orderId, CancellationToken cancellationToken)
    {
        return await dbContext.Orders
            .AsNoTracking()
            .Include(order => order.Customer)
            .Where(order => order.Id == orderId)
            .Select(order => new OrderDetailsDto(
                order.Id,
                order.Name,
                order.CustomerId,
                order.Customer.Name,
                order.Customer.Email,
                order.Amount,
                order.Currency,
                order.Status,
                order.CreatedAt,
                order.PaidAt))
            .SingleOrDefaultAsync(cancellationToken);
    }

    public async Task<Order?> GetForCheckoutAsync(Guid orderId, CancellationToken cancellationToken)
    {
        return await dbContext.Orders
            .Include(order => order.Customer)
            .SingleOrDefaultAsync(order => order.Id == orderId, cancellationToken);
    }
}
