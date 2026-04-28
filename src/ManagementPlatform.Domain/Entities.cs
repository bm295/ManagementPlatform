namespace ManagementPlatform.Domain;

public enum OrderStatus
{
    Draft = 0,
    CheckoutProcessing = 1,
    Paid = 2
}

public enum CheckoutStatus
{
    PaymentPending = 0,
    PaymentFailed = 1,
    PaymentSucceeded = 2
}

public enum PaymentStatus
{
    Failed = 0,
    Succeeded = 1
}

public enum InvoiceStatus
{
    Pending = 0,
    Succeeded = 1,
    Failed = 2
}

public enum OutboxStatus
{
    Pending = 0,
    Processing = 1,
    Succeeded = 2,
    Failed = 3
}

public enum OutboxMessageType
{
    SendCheckoutEmail = 0,
    PushToProduction = 1
}

public sealed class Tenant
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; }

    public List<Order> Orders { get; set; } = [];
}

public sealed class Order
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public string Name { get; set; } = string.Empty;
    public decimal Amount { get; set; }
    public string Currency { get; set; } = "USD";
    public OrderStatus Status { get; set; } = OrderStatus.Draft;
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? PaidAt { get; set; }

    public List<CheckoutAttempt> CheckoutAttempts { get; set; } = [];
}

public sealed class CheckoutAttempt
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid OrderId { get; set; }
    public Order Order { get; set; } = null!;
    public string IdempotencyKey { get; set; } = string.Empty;
    public CheckoutStatus Status { get; set; } = CheckoutStatus.PaymentPending;
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? CompletedAt { get; set; }

    public PaymentTransaction? PaymentTransaction { get; set; }
    public Invoice? Invoice { get; set; }
    public List<OutboxMessage> OutboxMessages { get; set; } = [];
}

public sealed class PaymentTransaction
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CheckoutAttemptId { get; set; }
    public CheckoutAttempt CheckoutAttempt { get; set; } = null!;
    public PaymentStatus Status { get; set; }
    public int AttemptCount { get; set; }
    public decimal Amount { get; set; }
    public string Currency { get; set; } = "USD";
    public string? ProviderTransactionId { get; set; }
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
}

public sealed class Invoice
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CheckoutAttemptId { get; set; }
    public CheckoutAttempt CheckoutAttempt { get; set; } = null!;
    public InvoiceStatus Status { get; set; } = InvoiceStatus.Pending;
    public string? FailureReason { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? CompletedAt { get; set; }
}

public sealed class OutboxMessage
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CheckoutAttemptId { get; set; }
    public CheckoutAttempt CheckoutAttempt { get; set; } = null!;
    public OutboxMessageType Type { get; set; }
    public OutboxStatus Status { get; set; } = OutboxStatus.Pending;
    public string PayloadJson { get; set; } = "{}";
    public int Attempts { get; set; }
    public string? LastError { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? NextAttemptAt { get; set; }
    public DateTimeOffset? LockedAt { get; set; }
    public DateTimeOffset? ProcessedAt { get; set; }
}

public sealed class DeadLetterMessage
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid OutboxMessageId { get; set; }
    public Guid CheckoutAttemptId { get; set; }
    public OutboxMessageType Type { get; set; }
    public string PayloadJson { get; set; } = "{}";
    public int AttemptCount { get; set; }
    public string FailureReason { get; set; } = string.Empty;
    public DateTimeOffset FailedAt { get; set; }
}
