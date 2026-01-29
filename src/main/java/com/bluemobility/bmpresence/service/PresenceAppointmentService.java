package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.exception.AppointmentConflictException;
import com.bluemobility.bmpresence.model.PresenceAppointment;
import com.bluemobility.bmpresence.repository.PresenceAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceAppointmentService {

    private final PresenceAppointmentRepository appointmentRepository;

    public List<PresenceAppointment> findAll() {
        return appointmentRepository.findAll();
    }

    public List<PresenceAppointment> findAllActive() {
        return appointmentRepository.findByActiveTrue();
    }

    public PresenceAppointment findById(Integer id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada con id: " + id));
    }

    public List<PresenceAppointment> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateRange(start, end);
    }

    public List<PresenceAppointment> findByResourceId(Integer resourceId) {
        return appointmentRepository.findByResourceId(resourceId);
    }

    @Transactional
    public PresenceAppointment create(PresenceAppointment appointment) {
        validateNoConflictingAppointment(appointment, null);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public PresenceAppointment update(Integer id, PresenceAppointment appointmentDetails) {
        PresenceAppointment appointment = findById(id);

        validateNoConflictingAppointment(appointmentDetails, id);

        appointment.setSubject(appointmentDetails.getSubject());
        appointment.setStartTime(appointmentDetails.getStartTime());
        appointment.setEndTime(appointmentDetails.getEndTime());
        appointment.setRecurrenceRule(appointmentDetails.getRecurrenceRule());
        appointment.setResourceIds(appointmentDetails.getResourceIds());
        appointment.setActive(appointmentDetails.getActive());

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void delete(Integer id) {
        appointmentRepository.deleteById(id);
    }

    private void validateNoConflictingAppointment(PresenceAppointment appointment, Integer currentAppointmentId) {
        if (appointment.getSubject() == null || appointment.getResourceIds() == null
                || appointment.getResourceIds().isEmpty()) {
            return;
        }

        LocalDate appointmentDate = appointment.getStartTime().toLocalDate();
        LocalDateTime startOfDay = appointmentDate.atStartOfDay();
        LocalDateTime endOfDay = appointmentDate.plusDays(1).atStartOfDay();

        for (Integer resourceId : appointment.getResourceIds()) {
            List<PresenceAppointment> conflicts = appointmentRepository.findConflictingAppointments(
                    appointment.getSubject(),
                    resourceId,
                    startOfDay,
                    endOfDay);

            // Filtrar el appointment actual si estamos actualizando
            if (currentAppointmentId != null) {
                conflicts = conflicts.stream()
                        .filter(a -> !a.getId().equals(currentAppointmentId))
                        .toList();
            }

            if (!conflicts.isEmpty()) {
                throw new AppointmentConflictException(
                        String.format("La persona '%s' ya tiene un appointment asignado para el recurso %d el día %s",
                                appointment.getSubject(), resourceId, appointmentDate));
            }
        }
    }
}
