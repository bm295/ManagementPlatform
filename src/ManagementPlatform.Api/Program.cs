using ManagementPlatform.Api;
using ManagementPlatform.Application;
using ManagementPlatform.Infrastructure;
using ManagementPlatform.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);
builder.Services.AddControllers();
builder.Services.AddOpenApi();
builder.Services.AddProblemDetails();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

if (app.Configuration.GetValue<bool>("Database:ApplyMigrationsOnStartup"))
{
    using var scope = app.Services.CreateScope();
    var dbContext = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
    await dbContext.Database.MigrateAsync();
    await scope.ServiceProvider.GetRequiredService<DbSeeder>().SeedAsync();
}

app.UseMiddleware<ApiExceptionMiddleware>();

if (!app.Environment.IsEnvironment("Testing"))
{
    app.UseHttpsRedirection();
}

app.UseDefaultFiles();
app.UseStaticFiles();

app.MapControllers();

app.Run();

public partial class Program;
