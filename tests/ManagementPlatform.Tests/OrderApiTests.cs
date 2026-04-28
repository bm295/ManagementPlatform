using System.Diagnostics;
using System.Net;
using System.Net.Http.Json;
using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using ManagementPlatform.Infrastructure.Persistence;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Testcontainers.MsSql;

namespace ManagementPlatform.Tests;

public sealed class OrderApiTests
{
    [Fact]
    public async Task Search_returns_seeded_orders()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();

        var result = await client.GetFromJsonAsync<PagedResult<OrderSummaryDto>>("/api/orders?name=catalog");

        Assert.NotNull(result);
        Assert.Equal(1, result.TotalCount);
        Assert.Equal("Spring Catalog Retouch", result.Items[0].Name);
    }

    [Fact]
    public async Task Checkout_success_creates_pending_integration_work()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();
        var orderId = await CreateDraftOrderAsync(factory.Services, "Checkout Success Order");
        var request = new CheckoutRequest($"api-success-{Guid.NewGuid():N}", "tok_success");

        var response = await client.PostAsJsonAsync(
            $"/api/orders/{orderId}/checkout",
            request);

        response.EnsureSuccessStatusCode();
        var checkout = await response.Content.ReadFromJsonAsync<CheckoutResponse>();

        Assert.NotNull(checkout);
        Assert.Equal(CheckoutStatus.PaymentSucceeded, checkout.Status);
        Assert.Equal(PaymentStatus.Succeeded, checkout.PaymentStatus);
        Assert.Equal(3, checkout.Integrations.Count);
        Assert.All(checkout.Integrations, item => Assert.Equal(OutboxStatus.Pending, item.Status));

        var fetched = await client.GetFromJsonAsync<CheckoutResponse>($"/api/checkouts/{checkout.CheckoutId}");
        Assert.Equal(checkout.CheckoutId, fetched!.CheckoutId);
    }

    [Fact]
    public async Task Checkout_failure_does_not_create_integration_work()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();
        var orderId = await CreateDraftOrderAsync(factory.Services, "Checkout Failure Order");
        var request = new CheckoutRequest($"api-failure-{Guid.NewGuid():N}", "tok_fail");

        var response = await client.PostAsJsonAsync(
            $"/api/orders/{orderId}/checkout",
            request);

        response.EnsureSuccessStatusCode();
        var checkout = await response.Content.ReadFromJsonAsync<CheckoutResponse>();

        Assert.NotNull(checkout);
        Assert.Equal(CheckoutStatus.PaymentFailed, checkout.Status);
        Assert.Equal(PaymentStatus.Failed, checkout.PaymentStatus);
        Assert.Empty(checkout.Integrations);
    }

    [Fact]
    public async Task Checkout_retryable_failure_can_succeed_within_retry_limit()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();
        var orderId = await CreateDraftOrderAsync(factory.Services, "Checkout Retry Success Order");
        var request = new CheckoutRequest($"api-retry-success-{Guid.NewGuid():N}", "tok_retry_success");

        var response = await client.PostAsJsonAsync(
            $"/api/orders/{orderId}/checkout",
            request);

        response.EnsureSuccessStatusCode();
        var checkout = await response.Content.ReadFromJsonAsync<CheckoutResponse>();

        Assert.NotNull(checkout);
        Assert.Equal(CheckoutStatus.PaymentSucceeded, checkout.Status);
        Assert.Equal(PaymentStatus.Succeeded, checkout.PaymentStatus);
    }

    [Fact]
    public async Task Checkout_new_key_for_paid_order_returns_conflict()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();
        var orderId = await CreateDraftOrderAsync(factory.Services, "Checkout Conflict Order");
        var firstRequest = new CheckoutRequest($"api-conflict-{Guid.NewGuid():N}", "tok_success");
        var secondRequest = new CheckoutRequest($"api-conflict-{Guid.NewGuid():N}", "tok_success");

        var first = await client.PostAsJsonAsync(
            $"/api/orders/{orderId}/checkout",
            firstRequest);
        first.EnsureSuccessStatusCode();

        var second = await client.PostAsJsonAsync(
            $"/api/orders/{orderId}/checkout",
            secondRequest);

        Assert.Equal(HttpStatusCode.Conflict, second.StatusCode);
    }

    [Fact]
    public async Task Dead_letters_returns_empty_list_when_nothing_failed()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();

        var result = await client.GetFromJsonAsync<List<DeadLetterMessageDto>>("/api/dead-letters");

        Assert.NotNull(result);
        Assert.Empty(result);
    }

    private static async Task<Guid> CreateDraftOrderAsync(IServiceProvider services, string name)
    {
        using var scope = services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
        var now = DateTimeOffset.UtcNow;

        var customer = new Customer
        {
            Id = Guid.NewGuid(),
            Name = $"{name} Customer",
            Email = $"{Guid.NewGuid():N}@example.test",
            CreatedAt = now
        };

        var order = new Order
        {
            Id = Guid.NewGuid(),
            CustomerId = customer.Id,
            Customer = customer,
            Name = name,
            Amount = 150m,
            Currency = "USD",
            Status = OrderStatus.Draft,
            CreatedAt = now
        };

        dbContext.Customers.Add(customer);
        dbContext.Orders.Add(order);
        await dbContext.SaveChangesAsync();

        return order.Id;
    }
}

public sealed class PlatformApiFactory : WebApplicationFactory<Program>
{
    private readonly MsSqlContainer _container;

    private PlatformApiFactory(MsSqlContainer container)
    {
        _container = container;
    }

    public static async Task<PlatformApiFactory?> CreateAsync()
    {
        if (!await DockerIsAvailableAsync())
        {
            return null;
        }

        MsSqlContainer? container = null;

        try
        {
            container = new MsSqlBuilder("mcr.microsoft.com/mssql/server:2022-latest")
                .WithPassword("Your_strong_password123")
                .Build();

            await container.StartAsync();

            var factory = new PlatformApiFactory(container);
            using var scope = factory.Services.CreateScope();
            var dbContext = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
            await dbContext.Database.MigrateAsync();
            await scope.ServiceProvider.GetRequiredService<DbSeeder>().SeedAsync();

            return factory;
        }
        catch
        {
            if (container is not null)
            {
                await container.DisposeAsync();
            }

            return null;
        }
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");
        builder.ConfigureAppConfiguration((_, configurationBuilder) =>
        {
            configurationBuilder.AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["ConnectionStrings:PlatformDatabase"] = _container.GetConnectionString(),
                ["Database:ApplyMigrationsOnStartup"] = "false",
                ["Outbox:Enabled"] = "false"
            });
        });
    }

    public override async ValueTask DisposeAsync()
    {
        await _container.DisposeAsync();
        await base.DisposeAsync();
    }

    private static async Task<bool> DockerIsAvailableAsync()
    {
        try
        {
            using var process = Process.Start(new ProcessStartInfo
            {
                FileName = "docker",
                Arguments = "info --format {{.ServerVersion}}",
                RedirectStandardError = true,
                RedirectStandardOutput = true,
                UseShellExecute = false,
                CreateNoWindow = true
            });

            if (process is null)
            {
                return false;
            }

            using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));
            await process.WaitForExitAsync(timeout.Token);
            return process.ExitCode == 0;
        }
        catch
        {
            return false;
        }
    }
}
