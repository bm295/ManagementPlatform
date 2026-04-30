using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using Microsoft.EntityFrameworkCore;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class DeadLetterRepository(ApplicationDbContext dbContext) : IDeadLetterRepository
{
    public void Add(DeadLetterMessage message)
    {
        dbContext.DeadLetterMessages.Add(message);
    }

    public async Task<IReadOnlyList<DeadLetterMessageDto>> GetRecentAsync(CancellationToken cancellationToken)
    {
        return await dbContext.DeadLetterMessages
            .AsNoTracking()
            .OrderByDescending(message => message.FailedAt)
            .Take(100)
            .Select(message => new DeadLetterMessageDto(
                message.Id,
                message.CheckoutAttemptId,
                message.OutboxMessageId,
                message.Type,
                message.AttemptCount,
                message.FailureReason,
                message.FailedAt))
            .ToListAsync(cancellationToken);
    }
}

