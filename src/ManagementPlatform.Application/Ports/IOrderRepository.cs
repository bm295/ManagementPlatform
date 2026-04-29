using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public interface IOrderRepository
{
    Task<PagedResult<OrderSummaryDto>> SearchAsync(
        string? name,
        int page,
        int pageSize,
        CancellationToken cancellationToken);

    Task<OrderDetailsDto?> GetDetailsAsync(long orderId, CancellationToken cancellationToken);

    Task<Order?> GetForCheckoutAsync(long orderId, CancellationToken cancellationToken);
}
