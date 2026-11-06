package rostyslav.sharuiev.reservationsystem.availability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rostyslav.sharuiev.reservationsystem.reservations.ReservationRepository;
import rostyslav.sharuiev.reservationsystem.reservations.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationAvailabilityService {
    private static final Logger log = LoggerFactory.getLogger(ReservationAvailabilityService.class);
    private final ReservationRepository repository;

    public ReservationAvailabilityService(ReservationRepository repository) {
        this.repository = repository;
    }

    public boolean isReservationAvailable(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Start day must be 1 day earlier than end date");
        }

        List<Long> conflictingIds = repository.findConflictReservationIds(
                roomId,
                startDate,
                endDate,
                ReservationStatus.APPROVED
        );

        if (conflictingIds.isEmpty()) {
            return true;
        }

        log.info("Conflict with ids: {}", conflictingIds);

        return false;
    }
}
