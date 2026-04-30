using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ManagementPlatform.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class AddPaymentRetryAndDeadLetter : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "AttemptCount",
                table: "PaymentTransactions",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.CreateTable(
                name: "DeadLetterMessages",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    OutboxMessageId = table.Column<long>(type: "bigint", nullable: false),
                    CheckoutAttemptId = table.Column<long>(type: "bigint", nullable: false),
                    Type = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: false),
                    PayloadJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    AttemptCount = table.Column<int>(type: "int", nullable: false),
                    FailureReason = table.Column<string>(type: "nvarchar(1000)", maxLength: 1000, nullable: false),
                    FailedAt = table.Column<DateTimeOffset>(type: "datetimeoffset", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DeadLetterMessages", x => x.Id);
                });

            migrationBuilder.CreateIndex(
                name: "IX_DeadLetterMessages_OutboxMessageId",
                table: "DeadLetterMessages",
                column: "OutboxMessageId",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "DeadLetterMessages");

            migrationBuilder.DropColumn(
                name: "AttemptCount",
                table: "PaymentTransactions");
        }
    }
}
