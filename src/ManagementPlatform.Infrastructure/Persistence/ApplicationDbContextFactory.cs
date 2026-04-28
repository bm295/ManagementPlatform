using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class ApplicationDbContextFactory : IDesignTimeDbContextFactory<ApplicationDbContext>
{
    public ApplicationDbContext CreateDbContext(string[] args)
    {
        var optionsBuilder = new DbContextOptionsBuilder<ApplicationDbContext>();
        optionsBuilder.UseSqlServer(
            "Server=localhost,1433;Database=ManagementPlatform;User Id=sa;Password=Your_strong_password123;TrustServerCertificate=True");

        return new ApplicationDbContext(optionsBuilder.Options);
    }
}
