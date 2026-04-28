using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using ManagementPlatform.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace ManagementPlatform.Tests;

public sealed class CheckoutServiceTests
{
    [Fact]
    public async Task SearchAsync_filters_orders_by_name_case_insensitively()
    {
        await using var dbContext = CreateDbContext();
        await SeedOrdersAsync(dbContext);

        var service = new OrderQueryService(new OrderRepository(dbContext));

        var result = await service.SearchAsync("catalog", 1, 20, CancellationToken.None);

        Assert.Single(result.Items);
        Assert.Equal("Spring Catalog Retouch", result.Items[0].Name);
    }

    [Fact]
    public async Task CheckoutAsync_successful_payment_marks_order_paid_and_creates_outbox_work()
    {
        await using var dbContext = CreateDbContext();
        var orderId = await SeedOrdersAsync(dbContext);
        var paymentGateway = new TestPaymentGateway(succeeds: true);
        var service = CreateCheckoutService(dbContext, paymentGateway);

        var response = await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("key-1", "tok_success"),
            CancellationToken.None);

        var order = await dbContext.Orders.SingleAsync(order => order.Id == orderId);

        Assert.Equal(CheckoutStatus.PaymentSucceeded, response.Status);
        Assert.Equal(PaymentStatus.Succeeded, response.PaymentStatus);
        Assert.Equal(OrderStatus.Paid, order.Status);
        Assert.Equal(3, await dbContext.OutboxMessages.CountAsync());
        Assert.Equal(1, await dbContext.InvoiceRequests.CountAsync());
        Assert.All(response.Integrations, item => Assert.Equal(OutboxStatus.Pending, item.Status));
    }

    [Fact]
    public async Task CheckoutAsync_failed_payment_records_failure_without_downstream_work()
    {
        await using var dbContext = CreateDbContext();
        var orderId = await SeedOrdersAsync(dbContext);
        var paymentGateway = new TestPaymentGateway(succeeds: false);
        var service = CreateCheckoutService(dbContext, paymentGateway);

        var response = await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("key-1", "tok_fail"),
            CancellationToken.None);

        var order = await dbContext.Orders.SingleAsync(order => order.Id == orderId);

        Assert.Equal(CheckoutStatus.PaymentFailed, response.Status);
        Assert.Equal(PaymentStatus.Failed, response.PaymentStatus);
        Assert.Equal(OrderStatus.Draft, order.Status);
        Assert.Equal("Declined", response.FailureReason);
        Assert.Empty(response.Integrations);
        Assert.Empty(dbContext.OutboxMessages);
        Assert.Empty(dbContext.InvoiceRequests);
    }

    [Fact]
    public async Task CheckoutAsync_same_idempotency_key_returns_original_attempt()
    {
        await using var dbContext = CreateDbContext();
        var orderId = await SeedOrdersAsync(dbContext);
        var paymentGateway = new TestPaymentGateway(succeeds: true);
        var service = CreateCheckoutService(dbContext, paymentGateway);

        var first = await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("same-key", "tok_success"),
            CancellationToken.None);
        var second = await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("same-key", "tok_success"),
            CancellationToken.None);

        Assert.Equal(first.CheckoutId, second.CheckoutId);
        Assert.Equal(1, paymentGateway.CallCount);
        Assert.Equal(1, await dbContext.CheckoutAttempts.CountAsync());
    }

    [Fact]
    public async Task CheckoutAsync_paid_order_with_new_key_returns_conflict()
    {
        await using var dbContext = CreateDbContext();
        var orderId = await SeedOrdersAsync(dbContext);
        var service = CreateCheckoutService(dbContext, new TestPaymentGateway(succeeds: true));

        await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("key-1", "tok_success"),
            CancellationToken.None);

        await Assert.ThrowsAsync<ConflictException>(() => service.CheckoutAsync(
            orderId,
            new CheckoutRequest("key-2", "tok_success"),
            CancellationToken.None));
    }

    [Fact]
    public async Task CheckoutAsync_retries_retryable_payment_failure_until_success()
    {
        await using var dbContext = CreateDbContext();
        var orderId = await SeedOrdersAsync(dbContext);
        var paymentGateway = new RetryablePaymentGateway(succeedOnAttempt: 3);
        var service = CreateCheckoutService(dbContext, paymentGateway, new PaymentRetryOptions
        {
            MaxAttempts = 3,
            DelayMilliseconds = 0
        });

        var response = await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("retry-key", "tok_retry_success"),
            CancellationToken.None);

        var payment = await dbContext.PaymentTransactions.SingleAsync();

        Assert.Equal(CheckoutStatus.PaymentSucceeded, response.Status);
        Assert.Equal(PaymentStatus.Succeeded, response.PaymentStatus);
        Assert.Equal(3, payment.AttemptCount);
        Assert.Equal(3, paymentGateway.CallCount);
    }

    [Fact]
    public async Task CheckoutAsync_stops_after_retry_limit_for_retryable_payment_failure()
    {
        await using var dbContext = CreateDbContext();
        var orderId = await SeedOrdersAsync(dbContext);
        var paymentGateway = new RetryablePaymentGateway(succeedOnAttempt: 10);
        var service = CreateCheckoutService(dbContext, paymentGateway, new PaymentRetryOptions
        {
            MaxAttempts = 3,
            DelayMilliseconds = 0
        });

        var response = await service.CheckoutAsync(
            orderId,
            new CheckoutRequest("retry-key-fail", "tok_retry_fail"),
            CancellationToken.None);

        var order = await dbContext.Orders.SingleAsync(order => order.Id == orderId);
        var payment = await dbContext.PaymentTransactions.SingleAsync();

        Assert.Equal(CheckoutStatus.PaymentFailed, response.Status);
        Assert.Equal(PaymentStatus.Failed, response.PaymentStatus);
        Assert.Equal(3, payment.AttemptCount);
        Assert.Equal(3, paymentGateway.CallCount);
        Assert.Equal(OrderStatus.Draft, order.Status);
        Assert.Empty(dbContext.OutboxMessages);
    }

    [Fact]
    public async Task OutboxDispatcher_moves_exhausted_message_to_dead_letter_store()
    {
        var databaseName = Guid.NewGuid().ToString();

        await using (var setupContext = CreateDbContext(databaseName))
        {
            var checkoutAttempt = new CheckoutAttempt
            {
                Id = Guid.NewGuid(),
                OrderId = Guid.NewGuid(),
                IdempotencyKey = "key",
                Status = CheckoutStatus.PaymentSucceeded,
                CreatedAt = DateTimeOffset.UtcNow
            };

            setupContext.CheckoutAttempts.Add(checkoutAttempt);
            setupContext.OutboxMessages.Add(new OutboxMessage
            {
                Id = Guid.NewGuid(),
                CheckoutAttemptId = checkoutAttempt.Id,
                Type = OutboxMessageType.SendCheckoutEmail,
                PayloadJson = "{}",
                Status = OutboxStatus.Pending,
                CreatedAt = DateTimeOffset.UtcNow
            });
            await setupContext.SaveChangesAsync();
        }

        var services = new ServiceCollection();
        services.AddLogging();
        services.AddSingleton<IClock>(new FixedClock());
        services.AddScoped(_ => CreateDbContext(databaseName));
        services.AddScoped<IEmailSender, FailingEmailSender>();
        services.AddScoped<IInvoiceClient, NoopInvoiceClient>();
        services.AddScoped<IProductionClient, NoopProductionClient>();

        var provider = services.BuildServiceProvider();
        var dispatcher = new ManagementPlatform.Infrastructure.Outbox.OutboxDispatcher(
            provider.GetRequiredService<IServiceScopeFactory>(),
            Microsoft.Extensions.Options.Options.Create(new ManagementPlatform.Infrastructure.Outbox.OutboxOptions
            {
                BatchSize = 10,
                MaxAttempts = 1,
                PollInterval = TimeSpan.FromMilliseconds(1)
            }),
            provider.GetRequiredService<Microsoft.Extensions.Logging.ILogger<ManagementPlatform.Infrastructure.Outbox.OutboxDispatcher>>());

        await dispatcher.DispatchBatchAsync(CancellationToken.None);

        await using var assertContext = CreateDbContext(databaseName);
        Assert.Single(assertContext.DeadLetterMessages);
        Assert.Equal(OutboxStatus.Failed, (await assertContext.OutboxMessages.SingleAsync()).Status);
    }

    private static ApplicationDbContext CreateDbContext(string? databaseName = null)
    {
        var options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase(databaseName ?? Guid.NewGuid().ToString())
            .Options;

        return new ApplicationDbContext(options);
    }

    private static CheckoutService CreateCheckoutService(
        ApplicationDbContext dbContext,
        IPaymentGateway paymentGateway,
        PaymentRetryOptions? paymentRetryOptions = null)
    {
        return new CheckoutService(
            new OrderRepository(dbContext),
            new CheckoutRepository(dbContext),
            dbContext,
            paymentGateway,
            new FixedClock(),
            paymentRetryOptions ?? new PaymentRetryOptions
            {
                MaxAttempts = 3,
                DelayMilliseconds = 0
            });
    }

    private static async Task<Guid> SeedOrdersAsync(ApplicationDbContext dbContext)
    {
        var now = DateTimeOffset.Parse("2026-04-28T00:00:00+00:00");
        var customer = new Customer
        {
            Id = Guid.NewGuid(),
            Name = "Northwind Studio",
            Email = "orders@northwind.example",
            CreatedAt = now
        };

        var firstOrder = new Order
        {
            Id = Guid.NewGuid(),
            CustomerId = customer.Id,
            Customer = customer,
            Name = "Spring Catalog Retouch",
            Amount = 250m,
            Currency = "USD",
            CreatedAt = now
        };

        dbContext.Customers.Add(customer);
        dbContext.Orders.AddRange(
            firstOrder,
            new Order
            {
                Id = Guid.NewGuid(),
                CustomerId = customer.Id,
                Customer = customer,
                Name = "Marketplace Launch Batch",
                Amount = 500m,
                Currency = "USD",
                CreatedAt = now.AddHours(1)
            });

        await dbContext.SaveChangesAsync();
        return firstOrder.Id;
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => DateTimeOffset.Parse("2026-04-28T00:00:00+00:00");
    }

    private sealed class TestPaymentGateway(bool succeeds) : IPaymentGateway
    {
        public int CallCount { get; private set; }

        public Task<PaymentGatewayResult> ChargeAsync(
            PaymentGatewayRequest request,
            CancellationToken cancellationToken)
        {
            CallCount++;

            return Task.FromResult(succeeds
                ? new PaymentGatewayResult(true, $"txn-{CallCount}", null)
                : new PaymentGatewayResult(false, null, "Declined"));
        }
    }

    private sealed class RetryablePaymentGateway(int succeedOnAttempt) : IPaymentGateway
    {
        public int CallCount { get; private set; }

        public Task<PaymentGatewayResult> ChargeAsync(
            PaymentGatewayRequest request,
            CancellationToken cancellationToken)
        {
            CallCount++;
            return Task.FromResult(CallCount >= succeedOnAttempt
                ? new PaymentGatewayResult(true, $"txn-{CallCount}", null)
                : new PaymentGatewayResult(false, null, "Temporary failure", true));
        }
    }

    private sealed class FailingEmailSender : IEmailSender
    {
        public Task SendCheckoutSucceededAsync(CheckoutEmailPayload payload, CancellationToken cancellationToken)
        {
            throw new InvalidOperationException("Email failed.");
        }
    }

    private sealed class NoopInvoiceClient : IInvoiceClient
    {
        public Task<InvoiceClientResult> CreateInvoiceAsync(InvoicePayload payload, CancellationToken cancellationToken)
        {
            return Task.FromResult(new InvoiceClientResult("inv"));
        }
    }

    private sealed class NoopProductionClient : IProductionClient
    {
        public Task PushOrderAsync(ProductionOrderPayload payload, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }
    }
}
