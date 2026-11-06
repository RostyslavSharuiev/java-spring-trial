package rostyslav.sharuiev.reservationsystem.availability;


public record CheckAvailabilityResponse(
    String message,
    AvailabilityStatus status
) {}
