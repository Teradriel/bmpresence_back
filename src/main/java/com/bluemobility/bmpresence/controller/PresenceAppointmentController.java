package com.bluemobility.bmpresence.controller;

import com.bluemobility.bmpresence.model.PresenceAppointment;
import com.bluemobility.bmpresence.service.PresenceAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PresenceAppointmentController {

    private final PresenceAppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<PresenceAppointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PresenceAppointment>> getActiveAppointments() {
        return ResponseEntity.ok(appointmentService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PresenceAppointment> getAppointmentById(@PathVariable Integer id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping("/range")
    public ResponseEntity<List<PresenceAppointment>> getAppointmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(appointmentService.findByDateRange(start, end));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<PresenceAppointment>> getAppointmentsByResource(@PathVariable Integer resourceId) {
        return ResponseEntity.ok(appointmentService.findByResourceId(resourceId));
    }

    @PostMapping
    public ResponseEntity<PresenceAppointment> createAppointment(@RequestBody PresenceAppointment appointment) {
        PresenceAppointment createdAppointment = appointmentService.create(appointment);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PresenceAppointment> updateAppointment(@PathVariable Integer id,
            @RequestBody PresenceAppointment appointment) {
        PresenceAppointment updatedAppointment = appointmentService.update(id, appointment);
        return ResponseEntity.ok(updatedAppointment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Integer id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteAppointment(@PathVariable Integer id) {
        appointmentService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
