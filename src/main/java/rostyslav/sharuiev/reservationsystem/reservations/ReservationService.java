package rostyslav.sharuiev.reservationsystem.reservations;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository repository;
    private final ReservationMapper mapper;

    public ReservationService(ReservationRepository repository, ReservationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<Reservation> searchAllByFilter(ReservationSearchFilter filter) {
        int pageSize = filter.pageSize() != null
                ? filter.pageSize()
                : 10;

        int pageNumber = filter.pageNumber() != null
                ? filter.pageNumber()
                : 0;

        var pageable = Pageable.ofSize(pageSize).withPage(pageNumber);

        List<ReservationEntity> allEntities = repository.searchAllByFilter(filter.roomId(), filter.userId(), pageable);

        return allEntities
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    public Reservation getReservationById(Long id) {
        ReservationEntity reservationEntity = repository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation with id: " + id
                ));

        return mapper.toDomain(reservationEntity);
    }

    public Reservation createReservation(Reservation reservation) {
        if (reservation.status() != null) {
            throw new IllegalArgumentException("Status should be empty");
        }

        if (!reservation.endDate().isAfter(reservation.startDate())) {
            throw new IllegalArgumentException("Start day must be 1 day earlier than end date");
        }

        var entityToSave = mapper.toEntity(reservation);
        entityToSave.setStatus(ReservationStatus.PENDING);

        var savedEntity = repository.save(entityToSave);

        return mapper.toDomain(savedEntity);
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

        var reservationToSave = mapper.toEntity(reservationToUpdate);
        reservationToSave.setStatus(ReservationStatus.PENDING);
        reservationToSave.setId(reservationEntity.getId());

        var updatedReservation = repository.save(reservationToSave);

        return mapper.toDomain(updatedReservation);
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

        var isConflict = isReservationConflict(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );

        if (isConflict) {
            throw new IllegalStateException("Cannot approve reservation with reason: conflict");
        }

        reservationEntity.setStatus(ReservationStatus.APPROVED);

        repository.save(reservationEntity);

        return mapper.toDomain(reservationEntity);
    }

    private boolean isReservationConflict(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Long> conflictingIds = repository.findConflictReservationIds(
                roomId,
                startDate,
                endDate,
                ReservationStatus.APPROVED
        );

        if (conflictingIds.isEmpty()) {
            return false;
        }

        log.info("Conflict with ids: {}", conflictingIds);

        return true;
    }
}
