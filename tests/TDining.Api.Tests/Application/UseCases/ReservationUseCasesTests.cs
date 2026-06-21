using TDining.Application.UseCases.Reservations;
using TDining.Domain.Reservations;
using TDining.Domain.Tables;

namespace TDining.Api.Tests.Application.UseCases;

public sealed class ReservationUseCasesTests
{
    [Fact]
    public async Task CreateReservationAsync_WhenCapacityExceeded_ReturnsValidationFailure()
    {
        var repository = new InMemoryReservationRepository();
        var useCases = new ReservationUseCases(repository, new FixedClock());
        var request = new CreateReservationRequest(
            CustomerName: "Ada Lovelace",
            PhoneNumber: "555-0100",
            PartySize: 9,
            ReservationTime: new DateTimeOffset(2026, 7, 1, 19, 0, 0, TimeSpan.Zero));

        var result = await useCases.CreateReservationAsync(request, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Equal("Validation", result.ErrorType);
        Assert.Contains("capacity", result.ErrorMessage, StringComparison.OrdinalIgnoreCase);
        Assert.Empty(repository.Reservations);
    }

    [Fact]
    public async Task CreateReservationAsync_WhenInputHasWhitespace_TrimsCustomerAndPhone()
    {
        var repository = new InMemoryReservationRepository();
        var useCases = new ReservationUseCases(repository, new FixedClock());
        var request = new CreateReservationRequest(
            CustomerName: "  Grace Hopper  ",
            PhoneNumber: "  555-0199  ",
            PartySize: 2,
            ReservationTime: new DateTimeOffset(2026, 7, 1, 18, 30, 0, TimeSpan.Zero));

        var result = await useCases.CreateReservationAsync(request, CancellationToken.None);

        Assert.True(result.IsSuccess);
        var reservation = Assert.Single(repository.Reservations);
        Assert.Equal("Grace Hopper", reservation.CustomerName);
        Assert.Equal("555-0199", reservation.PhoneNumber);
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => new(2026, 6, 21, 12, 0, 0, TimeSpan.Zero);
    }

    private sealed class InMemoryReservationRepository : IReservationRepository
    {
        public List<Reservation> Reservations { get; } = [];

        public Task<int> GetTotalReservedSeatsAsync(DateTimeOffset reservationTime, CancellationToken cancellationToken)
        {
            var reservedSeats = Reservations
                .Where(reservation => reservation.ReservationTime == reservationTime)
                .Sum(reservation => reservation.PartySize);

            return Task.FromResult(reservedSeats);
        }

        public Task AddAsync(Reservation reservation, CancellationToken cancellationToken)
        {
            Reservations.Add(reservation);
            return Task.CompletedTask;
        }
    }
}
