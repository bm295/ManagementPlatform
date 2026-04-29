namespace ManagementPlatform.Application;

public sealed class OrderQueryService(IOrderRepository orderRepository)
{
    public async Task<PagedResult<OrderSummaryDto>> SearchAsync(
        string? name,
        int page,
        int pageSize,
        CancellationToken cancellationToken)
    {
        page = Math.Max(page, 1);
        pageSize = Math.Clamp(pageSize, 1, 100);

        return await orderRepository.SearchAsync(name, page, pageSize, cancellationToken);
    }

    public async Task<OrderDetailsDto> GetByIdAsync(long orderId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetDetailsAsync(orderId, cancellationToken);
        return order ?? throw new NotFoundException("Order was not found.");
    }
}
