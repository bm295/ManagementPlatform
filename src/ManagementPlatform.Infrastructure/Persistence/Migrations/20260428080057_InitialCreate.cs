using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ManagementPlatform.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "customers",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Name = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    Email = table.Column<string>(type: "nvarchar(320)", maxLength: 320, nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_customers", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "orders",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    CustomerId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Name = table.Column<string>(type: "nvarchar(240)", maxLength: 240, nullable: false),
                    Amount = table.Column<decimal>(type: "decimal(18,2)", precision: 18, scale: 2, nullable: false),
                    Currency = table.Column<string>(type: "nvarchar(3)", maxLength: 3, nullable: false),
                    Status = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false),
                    PaidAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_orders", x => x.Id);
                    table.ForeignKey(
                        name: "FK_orders_customers_CustomerId",
                        column: x => x.CustomerId,
                        principalTable: "customers",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "checkout_attempts",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    OrderId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    IdempotencyKey = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: false),
                    Status = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    FailureReason = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false),
                    CompletedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_checkout_attempts", x => x.Id);
                    table.ForeignKey(
                        name: "FK_checkout_attempts_orders_OrderId",
                        column: x => x.OrderId,
                        principalTable: "orders",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "invoice_requests",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    CheckoutAttemptId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Status = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    ExternalInvoiceId = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    FailureReason = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false),
                    CompletedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_invoice_requests", x => x.Id);
                    table.ForeignKey(
                        name: "FK_invoice_requests_checkout_attempts_CheckoutAttemptId",
                        column: x => x.CheckoutAttemptId,
                        principalTable: "checkout_attempts",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "outbox_messages",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    CheckoutAttemptId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Type = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: false),
                    Status = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    PayloadJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    Attempts = table.Column<int>(type: "int", nullable: false),
                    LastError = table.Column<string>(type: "nvarchar(1000)", maxLength: 1000, nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false),
                    NextAttemptAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: true),
                    LockedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: true),
                    ProcessedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_outbox_messages", x => x.Id);
                    table.ForeignKey(
                        name: "FK_outbox_messages_checkout_attempts_CheckoutAttemptId",
                        column: x => x.CheckoutAttemptId,
                        principalTable: "checkout_attempts",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "payment_transactions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    CheckoutAttemptId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Status = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: false),
                    Amount = table.Column<decimal>(type: "decimal(18,2)", precision: 18, scale: 2, nullable: false),
                    Currency = table.Column<string>(type: "nvarchar(3)", maxLength: 3, nullable: false),
                    ProviderTransactionId = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    FailureReason = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_payment_transactions", x => x.Id);
                    table.ForeignKey(
                        name: "FK_payment_transactions_checkout_attempts_CheckoutAttemptId",
                        column: x => x.CheckoutAttemptId,
                        principalTable: "checkout_attempts",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_checkout_attempts_OrderId_IdempotencyKey",
                table: "checkout_attempts",
                columns: new[] { "OrderId", "IdempotencyKey" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_customers_Email",
                table: "customers",
                column: "Email",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_invoice_requests_CheckoutAttemptId",
                table: "invoice_requests",
                column: "CheckoutAttemptId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_invoice_requests_ExternalInvoiceId",
                table: "invoice_requests",
                column: "ExternalInvoiceId",
                unique: true,
                filter: "[ExternalInvoiceId] IS NOT NULL");

            migrationBuilder.CreateIndex(
                name: "IX_orders_CustomerId",
                table: "orders",
                column: "CustomerId");

            migrationBuilder.CreateIndex(
                name: "IX_orders_Name",
                table: "orders",
                column: "Name");

            migrationBuilder.CreateIndex(
                name: "IX_outbox_messages_CheckoutAttemptId",
                table: "outbox_messages",
                column: "CheckoutAttemptId");

            migrationBuilder.CreateIndex(
                name: "IX_outbox_messages_Status_NextAttemptAt",
                table: "outbox_messages",
                columns: new[] { "Status", "NextAttemptAt" });

            migrationBuilder.CreateIndex(
                name: "IX_payment_transactions_CheckoutAttemptId",
                table: "payment_transactions",
                column: "CheckoutAttemptId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_payment_transactions_ProviderTransactionId",
                table: "payment_transactions",
                column: "ProviderTransactionId",
                unique: true,
                filter: "[ProviderTransactionId] IS NOT NULL");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "invoice_requests");

            migrationBuilder.DropTable(
                name: "outbox_messages");

            migrationBuilder.DropTable(
                name: "payment_transactions");

            migrationBuilder.DropTable(
                name: "checkout_attempts");

            migrationBuilder.DropTable(
                name: "orders");

            migrationBuilder.DropTable(
                name: "customers");
        }
    }
}
