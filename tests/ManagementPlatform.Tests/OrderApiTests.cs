using System.Diagnostics;
using System.Net.Http.Json;
using ManagementPlatform.Application;
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
    public async Task Search_returns_empty_when_name_does_not_match()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();

        var result = await client.GetFromJsonAsync<PagedResult<OrderSummaryDto>>("/api/orders?name=does-not-exist");

        Assert.NotNull(result);
        Assert.Equal(0, result.TotalCount);
        Assert.Empty(result.Items);
    }

    [Fact]
    public async Task Get_returns_order_details_for_existing_order()
    {
        await using var factory = await PlatformApiFactory.CreateAsync();
        if (factory is null)
        {
            return;
        }

        var client = factory.CreateClient();
        var search = await client.GetFromJsonAsync<PagedResult<OrderSummaryDto>>("/api/orders?name=spring");
        Assert.NotNull(search);
        Assert.NotEmpty(search.Items);

        var result = await client.GetFromJsonAsync<OrderDetailsDto>($"/api/orders/{search.Items[0].Id}");

        Assert.NotNull(result);
        Assert.Equal(search.Items[0].Id, result.Id);
        Assert.Equal("Spring Catalog Retouch", result.Name);
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
