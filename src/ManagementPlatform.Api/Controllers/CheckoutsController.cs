using ManagementPlatform.Application;
using Microsoft.AspNetCore.Mvc;

namespace ManagementPlatform.Api.Controllers;

[ApiController]
[Route("api/checkouts")]
public sealed class CheckoutsController(CheckoutService checkoutService) : ControllerBase
{
    [HttpGet("{checkoutId:guid}")]
    [ProducesResponseType<CheckoutResponse>(StatusCodes.Status200OK)]
    [ProducesResponseType<ProblemDetails>(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<CheckoutResponse>> Get(
        long checkoutId,
        CancellationToken cancellationToken)
    {
        return Ok(await checkoutService.GetAsync(checkoutId, cancellationToken));
    }
}
