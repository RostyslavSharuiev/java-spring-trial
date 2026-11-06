package rostyslav.sharuiev.reservationsystem.reservations;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    public List<Reservation> findAllReservations() {
        List<ReservationEntity> allEntities = repository.findAll();

        return allEntities
                .stream()
                .map(this::toDomainReservation)
                .toList();
    }

    public Reservation getReservationById(Long id) {
        ReservationEntity reservationEntity = repository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation with id: " + id
                ));

        return toDomainReservation(reservationEntity);
    }

    public Reservation createReservation(Reservation reservation) {
        if (reservation.status() != null) {
            throw new IllegalArgumentException("Status should be empty");
        }

        if (!reservation.endDate().isAfter(reservation.startDate())) {
            throw new IllegalArgumentException("Start day must be 1 day earlier than end date");
        }

        var entityToSave = new ReservationEntity(
               null,
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.PENDING
        );

        var savedEntity = repository.save(entityToSave);

        return toDomainReservation(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = repository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation with id: " + id
                ));

        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot modify reservation with status " + reservationEntity.getStatus()
            );
        }

        if (!reservationToUpdate.endDate().isAfter(reservationToUpdate.startDate())) {
            throw new IllegalArgumentException("Start day must be 1 day earlier than end date");
        }

        var reservationToSave = new ReservationEntity(
                reservationEntity.getId(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );

        var updatedReservation = repository.save(reservationToSave);

        return toDomainReservation(updatedReservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Not found reservation with id: " + id);
        }

        var reservation = repository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation with id: " + id
                ));

        if (reservation.getStatus().equals(ReservationStatus.APPROVED)) {
            throw new IllegalStateException("Can not cancel approved reservation. Contact with manager please");
        }

        if (reservation.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new IllegalStateException("Can not cancel the reservation. Reservation was already cancelled");
        }

        repository.setStatus(id, ReservationStatus.CANCELLED);

        log.info("Successfully canceled reservation with id = {}", id);
    }

    public Reservation approveReservation(Long id) {
        var reservationEntity = repository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation with id: " + id
                ));

        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve reservation with status " + reservationEntity.getStatus()
            );
        }

        var isConflict = isReservationConflict(reservationEntity);

        if (isConflict) {
            throw new IllegalStateException("Cannot approve reservation with reason: conflict");
        }

        reservationEntity.setStatus(ReservationStatus.APPROVED);

        repository.save(reservationEntity);

        return toDomainReservation(reservationEntity);
    }

    private boolean isReservationConflict(ReservationEntity reservation) {
        var allReservations = repository.findAll();

        for (ReservationEntity existingReservation : allReservations) {
            if (reservation.getId().equals(existingReservation.getId())) {
                continue;
            }

            if (!reservation.getRoomId().equals(existingReservation.getRoomId())) {
                continue;
            }

            if (!existingReservation.getStatus().equals(ReservationStatus.APPROVED)) {
                continue;
            }

            if (reservation.getStartDate().isBefore(existingReservation.getEndDate())
                    && existingReservation.getStartDate().isBefore(reservation.getEndDate())) {
                return true;
            }
        }

        return false;
    }

    private Reservation toDomainReservation(ReservationEntity reservation) {
        return new Reservation(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getRoomId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus()
        );
    }
}
