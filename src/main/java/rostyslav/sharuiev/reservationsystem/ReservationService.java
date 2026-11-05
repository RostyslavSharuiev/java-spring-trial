package rostyslav.sharuiev.reservationsystem;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReservationService {
    private final Map<Long, Reservation> reservationMap;
    private final AtomicLong idCounter;

    public ReservationService() {
        reservationMap = new HashMap<>();
        idCounter = new AtomicLong();
    }

    public List<Reservation> findAllReservations() {
        return reservationMap.values().stream().toList();
    }

    public Reservation getReservationById(Long id) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation with id: " + id);
        }

        return reservationMap.get(id);
    }

    public Reservation createReservation(Reservation reservation) {
        if (reservation.id() != null) {
            throw new IllegalArgumentException("Id should be empty");
        }

        if (reservation.status() != null) {
            throw new IllegalArgumentException("Status should be empty");
        }

        var newReservation = new Reservation(
                idCounter.incrementAndGet(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.PENDING
        );

        reservationMap.put(newReservation.id(), newReservation);

        return newReservation;
    }

    public Reservation updateReservation(Long id, Reservation reservation) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation with id: " + id);
        }

        var olrReservation = reservationMap.get(id);

        if (olrReservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot modify reservation with status " + olrReservation.status());
        }

        var updatedReservation = new Reservation(
                olrReservation.id(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.PENDING
        );

        reservationMap.put(olrReservation.id(), updatedReservation);

        return updatedReservation;
    }

    public void deleteReservation(Long id) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation with id: " + id);
        }

        reservationMap.remove(id);
    }

    public Reservation approveReservation(Long id) {
        if (!reservationMap.containsKey(id)) {
            throw new NoSuchElementException("Not found reservation with id: " + id);
        }

        var reservation = reservationMap.get(id);

        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot approve reservation with status " + reservation.status());
        }

        var isConflict = isReservationConflict(reservation);

        if (isConflict) {
            throw new IllegalStateException("Cannot approve reservation with reason: conflict");
        }

        var approvedReservation = new Reservation(
                reservation.id(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.APPROVED
        );

        reservationMap.put(reservation.id(), approvedReservation);

        return approvedReservation;
    }

    private boolean isReservationConflict(Reservation reservation) {
        for (Reservation existingReservation : reservationMap.values()) {
            if (reservation.id().equals(existingReservation.id())) {
                continue;
            }

            if (!reservation.roomId().equals(existingReservation.roomId())) {
                continue;
            }

            if (!existingReservation.status().equals(ReservationStatus.APPROVED)) {
                continue;
            }

            if (reservation.startDate().isBefore(existingReservation.endDate())
                    && existingReservation.startDate().isBefore(reservation.endDate())) {
                return true;
            }
        }

        return false;
    }
}
