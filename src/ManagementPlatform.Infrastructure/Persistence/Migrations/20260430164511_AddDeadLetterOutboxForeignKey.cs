using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ManagementPlatform.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class AddDeadLetterOutboxForeignKey : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddForeignKey(
                name: "FK_DeadLetterMessages_OutboxMessages_OutboxMessageId",
                table: "DeadLetterMessages",
                column: "OutboxMessageId",
                principalTable: "OutboxMessages",
                principalColumn: "Id",
                onDelete: ReferentialAction.Cascade);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_DeadLetterMessages_OutboxMessages_OutboxMessageId",
                table: "DeadLetterMessages");
        }
    }
}
