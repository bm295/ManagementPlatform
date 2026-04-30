using System.Text.Json;
using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using ManagementPlatform.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Configuration;

namespace ManagementPlatform.Infrastructure.Outbox;

public sealed class OutboxDispatcher(
    IServiceScopeFactory scopeFactory,
    IConfiguration configuration,
    ILogger<OutboxDispatcher> logger) : BackgroundService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly int batchSize = Math.Max(configuration.GetValue<int?>("Outbox:BatchSize") ?? 25, 1);
    private readonly int maxAttempts = Math.Max(configuration.GetValue<int?>("Outbox:MaxAttempts") ?? 5, 1);
    private readonly TimeSpan pollInterval = configuration.GetValue<TimeSpan?>("Outbox:PollInterval") ?? TimeSpan.FromSeconds(5);

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await DispatchBatchAsync(stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                return;
            }
            catch (Exception exception)
            {
                logger.LogError(exception, "Outbox dispatch loop failed.");
            }

            await Task.Delay(pollInterval, stoppingToken);
        }
    }

    public async Task DispatchBatchAsync(CancellationToken cancellationToken)
    {
        using var scope = scopeFactory.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
        var deadLetterRepository = scope.ServiceProvider.GetRequiredService<IDeadLetterRepository>();
        var clock = scope.ServiceProvider.GetRequiredService<IClock>();

        var now = clock.UtcNow;
        var messages = await dbContext.OutboxMessages
            .Where(message => message.Status == OutboxStatus.Pending)
            .Where(message => message.NextAttemptAt == null || message.NextAttemptAt <= now)
            .OrderBy(message => message.CreatedAt)
            .Take(batchSize)
            .ToListAsync(cancellationToken);

        foreach (var message in messages)
        {
            await DispatchMessageAsync(scope.ServiceProvider, dbContext, deadLetterRepository, message, cancellationToken);
        }
    }

    private async Task DispatchMessageAsync(
        IServiceProvider serviceProvider,
        ApplicationDbContext dbContext,
        IDeadLetterRepository deadLetterRepository,
        OutboxMessage message,
        CancellationToken cancellationToken)
    {
        var clock = serviceProvider.GetRequiredService<IClock>();

        message.Status = OutboxStatus.Processing;
        message.AttemptCount++;
        message.LockedAt = clock.UtcNow;
        await dbContext.SaveChangesAsync(cancellationToken);

        try
        {
            await DispatchByTypeAsync(serviceProvider, dbContext, message, cancellationToken);

            message.Status = OutboxStatus.Succeeded;
            message.LastError = null;
            message.LockedAt = null;
            message.ProcessedAt = clock.UtcNow;
            message.NextAttemptAt = null;
        }
        catch (Exception exception)
        {
            logger.LogWarning(
                exception,
                "Outbox message {MessageId} failed on attempt {Attempt}.",
                message.Id,
                message.AttemptCount);

            message.LastError = exception.Message;
            message.LockedAt = null;

            if (message.AttemptCount >= maxAttempts)
            {
                message.Status = OutboxStatus.Failed;
                deadLetterRepository.Add(new DeadLetterMessage
                {
                    OutboxMessageId = message.Id,
                    CheckoutAttemptId = message.CheckoutAttemptId,
                    Type = message.Type,
                    PayloadJson = message.PayloadJson,
                    AttemptCount = message.AttemptCount,
                    FailureReason = exception.Message,
                    FailedAt = clock.UtcNow
                });
            }
            else
            {
                message.Status = OutboxStatus.Pending;
                message.NextAttemptAt = clock.UtcNow.AddSeconds(Math.Min(Math.Pow(2, message.AttemptCount), 60));
            }

            await MarkInvoiceFailureAsync(dbContext, message, exception.Message, cancellationToken);
            await dbContext.SaveChangesAsync(cancellationToken);
            return;
        }

        await dbContext.SaveChangesAsync(cancellationToken);
    }

    private static async Task DispatchByTypeAsync(
        IServiceProvider serviceProvider,
        ApplicationDbContext dbContext,
        OutboxMessage message,
        CancellationToken cancellationToken)
    {
        var clock = serviceProvider.GetRequiredService<IClock>();

        switch (message.Type)
        {
            case OutboxMessageType.SendCheckoutEmail:
                var emailPayload = Deserialize<CheckoutEmailPayload>(message.PayloadJson);
                await serviceProvider.GetRequiredService<IEmailSender>()
                    .SendCheckoutSucceededAsync(emailPayload, cancellationToken);
                break;

            case OutboxMessageType.PushToProduction:
                var productionPayload = Deserialize<ProductionOrderPayload>(message.PayloadJson);
                await serviceProvider.GetRequiredService<IProductionClient>()
                    .PushOrderAsync(productionPayload, cancellationToken);
                var invoice = await dbContext.Invoices
                    .SingleAsync(item => item.CheckoutAttemptId == message.CheckoutAttemptId, cancellationToken);
                invoice.Status = InvoiceStatus.Succeeded;
                invoice.FailureReason = null;
                invoice.CompletedAt = clock.UtcNow;
                break;

            default:
                throw new InvalidOperationException($"Unsupported outbox message type '{message.Type}'.");
        }
    }

    private static async Task MarkInvoiceFailureAsync(
        ApplicationDbContext dbContext,
        OutboxMessage message,
        string failureReason,
        CancellationToken cancellationToken)
    {
        if (message.Type is not OutboxMessageType.PushToProduction)
        {
            return;
        }

        var invoice = await dbContext.Invoices
            .SingleOrDefaultAsync(item => item.CheckoutAttemptId == message.CheckoutAttemptId, cancellationToken);

        if (invoice is null)
        {
            return;
        }

        invoice.Status = InvoiceStatus.Failed;
        invoice.FailureReason = failureReason;
    }

    private static TPayload Deserialize<TPayload>(string json)
    {
        return JsonSerializer.Deserialize<TPayload>(json, JsonOptions)
            ?? throw new InvalidOperationException("Outbox payload could not be read.");
    }
}
