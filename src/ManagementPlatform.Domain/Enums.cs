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
