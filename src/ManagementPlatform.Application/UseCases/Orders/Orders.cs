using ManagementPlatform.Domain;

namespace ManagementPlatform.Application;

public sealed record PagedResult<T>(
    IReadOnlyList<T> Items,
    int Page,
    int PageSize,
    int TotalCount);

public sealed record OrderSummaryDto(
    Guid Id,
    string Name,
    string CustomerName,
    decimal Amount,
    string Currency,
    OrderStatus Status,
    DateTimeOffset CreatedAt);

public sealed record OrderDetailsDto(
    Guid Id,
    string Name,
    Guid CustomerId,
    string CustomerName,
    string CustomerEmail,
    decimal Amount,
    string Currency,
    OrderStatus Status,
    DateTimeOffset CreatedAt,
    DateTimeOffset? PaidAt);

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

    public async Task<OrderDetailsDto> GetByIdAsync(Guid orderId, CancellationToken cancellationToken)
    {
        var order = await orderRepository.GetDetailsAsync(orderId, cancellationToken);
        return order ?? throw new NotFoundException("Order was not found.");
    }
}
