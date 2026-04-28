using ManagementPlatform.Application;
using ManagementPlatform.Infrastructure.Outbox;
using ManagementPlatform.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;

namespace ManagementPlatform.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("PlatformDatabase")
            ?? throw new InvalidOperationException("Connection string 'PlatformDatabase' is required.");

        services.AddDbContext<ApplicationDbContext>(options => options.UseSqlServer(connectionString));
        services.AddScoped<IUnitOfWork>(provider => provider.GetRequiredService<ApplicationDbContext>());
        services.AddScoped<IOrderRepository, OrderRepository>();
        services.AddScoped<ICheckoutRepository, CheckoutRepository>();
        services.AddScoped<IDeadLetterRepository, DeadLetterRepository>();
        services.AddScoped<DbSeeder>();

        services.AddSingleton<IClock, SystemClock>();
        services.Configure<PaymentRetryOptions>(configuration.GetSection("PaymentRetry"));
        services.AddSingleton(provider => provider.GetRequiredService<IOptions<PaymentRetryOptions>>().Value);
        services.Configure<MockIntegrationOptions>(configuration.GetSection("MockIntegrations"));
        services.Configure<OutboxOptions>(configuration.GetSection("Outbox"));

        services.AddScoped<IPaymentGateway, MockPaymentGateway>();
        services.AddScoped<IEmailSender, MockEmailSender>();
        services.AddScoped<IProductionClient, MockProductionClient>();

        var outboxEnabled = configuration.GetValue<bool?>("Outbox:Enabled") ?? true;
        if (outboxEnabled)
        {
            services.AddHostedService<OutboxDispatcher>();
        }

        return services;
    }
}
