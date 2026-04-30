using ManagementPlatform.Application;
using ManagementPlatform.Domain;
using Microsoft.EntityFrameworkCore;

namespace ManagementPlatform.Infrastructure.Persistence;

public sealed class ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
    : DbContext(options), IAppDbContext
{
    public DbSet<Tenant> Tenants => Set<Tenant>();
    public DbSet<Order> Orders => Set<Order>();
    public DbSet<CheckoutAttempt> CheckoutAttempts => Set<CheckoutAttempt>();
    public DbSet<PaymentTransaction> PaymentTransactions => Set<PaymentTransaction>();
    public DbSet<Invoice> Invoices => Set<Invoice>();
    public DbSet<OutboxMessage> OutboxMessages => Set<OutboxMessage>();
    public DbSet<DeadLetterMessage> DeadLetterMessages => Set<DeadLetterMessage>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Tenant>(entity =>
        {
            entity.ToTable("Tenants");
            entity.HasKey(tenant => tenant.Id);
            entity.Property(tenant => tenant.Name).HasMaxLength(200).IsRequired();
            entity.Property(tenant => tenant.Email).HasMaxLength(320).IsRequired();
        });

        modelBuilder.Entity<Order>(entity =>
        {
            entity.ToTable("Orders");
            entity.HasKey(order => order.Id);
            entity.Property(order => order.Name).HasMaxLength(240).IsRequired();
            entity.Property(order => order.Amount).HasPrecision(18, 2);
            entity.Property(order => order.Currency).HasMaxLength(3).IsRequired();
            entity.Property(order => order.Status).HasConversion<byte>().HasColumnType("tinyint");
            entity.HasIndex(order => order.Name);
            entity.HasOne(order => order.Tenant)
                .WithMany(tenant => tenant.Orders)
                .HasForeignKey(order => order.TenantId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<CheckoutAttempt>(entity =>
        {
            entity.ToTable("CheckoutAttempts");
            entity.HasKey(attempt => attempt.Id);
            entity.Property(attempt => attempt.IdempotencyKey).HasMaxLength(120).IsRequired();
            entity.Property(attempt => attempt.Status).HasConversion<byte>().HasColumnType("tinyint");
            entity.Property(attempt => attempt.FailureReason).HasMaxLength(500);
            entity.HasIndex(attempt => new { attempt.OrderId, attempt.IdempotencyKey }).IsUnique();
            entity.HasOne(attempt => attempt.Order)
                .WithMany(order => order.CheckoutAttempts)
                .HasForeignKey(attempt => attempt.OrderId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<PaymentTransaction>(entity =>
        {
            entity.ToTable("PaymentTransactions");
            entity.HasKey(transaction => transaction.Id);
            entity.Property(transaction => transaction.Status).HasConversion<byte>().HasColumnType("tinyint");
            entity.Property(transaction => transaction.AttemptCount).IsRequired();
            entity.Property(transaction => transaction.Amount).HasPrecision(18, 2);
            entity.Property(transaction => transaction.Currency).HasMaxLength(3).IsRequired();
            entity.Property(transaction => transaction.ProviderTransactionId).HasMaxLength(120);
            entity.Property(transaction => transaction.FailureReason).HasMaxLength(500);
            entity.HasIndex(transaction => transaction.ProviderTransactionId).IsUnique();
            entity.HasOne(transaction => transaction.CheckoutAttempt)
                .WithOne(attempt => attempt.PaymentTransaction)
                .HasForeignKey<PaymentTransaction>(transaction => transaction.CheckoutAttemptId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Invoice>(entity =>
        {
            entity.ToTable("Invoices");
            entity.HasKey(invoice => invoice.Id);
            entity.Property(invoice => invoice.Status).HasConversion<byte>().HasColumnType("tinyint");
            entity.Property(invoice => invoice.FailureReason).HasMaxLength(500);
            entity.HasOne(invoice => invoice.CheckoutAttempt)
                .WithOne(attempt => attempt.Invoice)
                .HasForeignKey<Invoice>(invoice => invoice.CheckoutAttemptId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<OutboxMessage>(entity =>
        {
            entity.ToTable("OutboxMessages");
            entity.HasKey(message => message.Id);
            entity.Property(message => message.Type).HasConversion<string>().HasMaxLength(60);
            entity.Property(message => message.Status).HasConversion<byte>().HasColumnType("tinyint");
            entity.Property(message => message.PayloadJson).HasColumnType("nvarchar(max)").IsRequired();
            entity.Property(message => message.LastError).HasMaxLength(1000);
            entity.HasIndex(message => new { message.Status, message.NextAttemptAt });
            entity.HasOne(message => message.CheckoutAttempt)
                .WithMany(attempt => attempt.OutboxMessages)
                .HasForeignKey(message => message.CheckoutAttemptId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(message => message.DeadLetterMessage)
                .WithOne(deadLetter => deadLetter.OutboxMessage)
                .HasForeignKey<DeadLetterMessage>(deadLetter => deadLetter.OutboxMessageId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<DeadLetterMessage>(entity =>
        {
            entity.ToTable("DeadLetterMessages");
            entity.HasKey(message => message.Id);
            entity.Property(message => message.Type).HasConversion<string>().HasMaxLength(60);
            entity.Property(message => message.PayloadJson).HasColumnType("nvarchar(max)").IsRequired();
            entity.Property(message => message.FailureReason).HasMaxLength(1000).IsRequired();
            entity.HasIndex(message => message.OutboxMessageId).IsUnique();
        });
    }
}
