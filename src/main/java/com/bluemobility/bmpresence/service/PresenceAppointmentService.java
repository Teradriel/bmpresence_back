package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.model.PresenceAppointment;
import com.bluemobility.bmpresence.repository.PresenceAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
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
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public PresenceAppointment update(Integer id, PresenceAppointment appointmentDetails) {
        PresenceAppointment appointment = findById(id);

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
        PresenceAppointment appointment = findById(id);
        appointment.setActive(false);
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void hardDelete(Integer id) {
        appointmentRepository.deleteById(id);
    }
}
