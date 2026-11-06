package rostyslav.sharuiev.reservationsystem.web;

import java.time.LocalDateTime;

public record ErrorResponseDto (
        String message,
        String detailedMessage,
        LocalDateTime errorTime
) {
}
