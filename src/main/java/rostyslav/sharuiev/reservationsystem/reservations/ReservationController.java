package rostyslav.sharuiev.reservationsystem.reservations;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Reservation>> getAllReservations(
            @RequestParam(name = "roomId", required = false) Long roomId,
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "pageNumber", required = false) Integer pageNumber
    ) {
        log.info("Called getAllReservations");

        var filter = new ReservationSearchFilter(
                roomId,
                userId,
                pageSize,
                pageNumber
        );

        return ResponseEntity.ok(reservationService.searchAllByFilter(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        log.info("Called getReservationById with id: {}", id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(reservationService.getReservationById(id));
    }

    @PostMapping("/")
    public ResponseEntity<Reservation> createReservation(@RequestBody @Valid Reservation reservation) {
        log.info("Called createReservation");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.createReservation(reservation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(
            @PathVariable Long id,
            @RequestBody @Valid Reservation reservation
    ) {
        log.info("Called updateReservation with:\tid = {} \tdata = {}", id, reservation);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(reservationService.updateReservation(id, reservation));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Void> deleteReservationById(@PathVariable Long id) {
        log.info("Called deleteReservationById with id: {}", id);

        reservationService.cancelReservation(id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Reservation> approveReservation(@PathVariable Long id) {
        log.info("Called approveReservation with id: {}", id);

        var reservation = reservationService.approveReservation(id);

        return ResponseEntity.ok(reservation);
    }
}
