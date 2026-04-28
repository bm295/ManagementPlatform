using Microsoft.Extensions.DependencyInjection;

namespace ManagementPlatform.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<OrderQueryService>();
        services.AddScoped<CheckoutService>();
        services.AddScoped<DeadLetterQueryService>();

        return services;
    }
}
