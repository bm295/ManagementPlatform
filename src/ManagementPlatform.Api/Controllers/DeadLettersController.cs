using ManagementPlatform.Application;
using Microsoft.AspNetCore.Mvc;

namespace ManagementPlatform.Api.Controllers;

[ApiController]
[Route("api/dead-letters")]
public sealed class DeadLettersController(DeadLetterQueryService deadLetterQueryService) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType<IReadOnlyList<DeadLetterMessageDto>>(StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<DeadLetterMessageDto>>> Get(CancellationToken cancellationToken)
    {
        return Ok(await deadLetterQueryService.GetRecentAsync(cancellationToken));
    }
}
