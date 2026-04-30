using ManagementPlatform.Application;
using Microsoft.AspNetCore.Mvc;

namespace ManagementPlatform.Api.Controllers;

[ApiController]
[Route("api/orders")]
public sealed class OrdersController(
    OrderQueryService orderQueryService,
    CheckoutService checkoutService) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType<PagedResult<OrderSummaryDto>>(StatusCodes.Status200OK)]
    public async Task<ActionResult<PagedResult<OrderSummaryDto>>> Search(
        [FromQuery] string? name,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20,
        CancellationToken cancellationToken = default)
    {
        return Ok(await orderQueryService.SearchAsync(name, page, pageSize, cancellationToken));
    }

    [HttpGet("{orderId:long}")]
    [ProducesResponseType<OrderDetailsDto>(StatusCodes.Status200OK)]
    [ProducesResponseType<ProblemDetails>(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<OrderDetailsDto>> Get(
        long orderId,
        CancellationToken cancellationToken)
    {
        return Ok(await orderQueryService.GetByIdAsync(orderId, cancellationToken));
    }

    [HttpPost("{orderId:long}/checkout")]
    [ProducesResponseType<CheckoutResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType<ProblemDetails>(StatusCodes.Status400BadRequest)]
    [ProducesResponseType<ProblemDetails>(StatusCodes.Status404NotFound)]
    [ProducesResponseType<ProblemDetails>(StatusCodes.Status409Conflict)]
    public async Task<ActionResult<CheckoutResponse>> Checkout(
        long orderId,
        [FromBody] CheckoutRequest request,
        CancellationToken cancellationToken)
    {
        return Ok(await checkoutService.CheckoutAsync(orderId, request, cancellationToken));
    }
}
