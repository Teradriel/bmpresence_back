package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.exception.AppointmentConflictException;
import com.bluemobility.bmpresence.model.PresenceAppointment;
import com.bluemobility.bmpresence.repository.PresenceAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import com.bluemobility.bmpresence.dto.AppointmentOccurrence;
import com.bluemobility.bmpresence.exception.InvalidAppointmentException;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

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
                .orElseThrow(() -> new RuntimeException("Appuntamento non trovato con id: " + id));
    }

    public List<PresenceAppointment> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateRange(start, end);
    }

    public List<PresenceAppointment> findByResourceId(Integer resourceId) {
        return appointmentRepository.findByResourceId(resourceId);
    }

    @Transactional
    public PresenceAppointment create(PresenceAppointment appointment) {
        validateRecurrence(appointment);
        validateNoConflictingAppointment(appointment, null);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public PresenceAppointment update(Integer id, PresenceAppointment appointmentDetails) {
        PresenceAppointment appointment = findById(id);

        validateRecurrence(appointmentDetails);
        validateNoConflictingAppointment(appointmentDetails, id);

        appointment.setSubject(appointmentDetails.getSubject());
        appointment.setUserId(appointmentDetails.getUserId());
        appointment.setRecurrenceExceptions(appointmentDetails.getRecurrenceExceptions());
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
        if ((appointment.getUserId() == null && appointment.getSubject() == null) || appointment.getResourceIds() == null
                || appointment.getResourceIds().isEmpty()) {
            return;
        }

        LocalDate appointmentDate = appointment.getStartTime().toLocalDate();
        LocalDateTime startOfDay = appointmentDate.atStartOfDay();
        LocalDateTime endOfDay = appointmentDate.plusDays(1).atStartOfDay();

        for (Integer resourceId : appointment.getResourceIds()) {
            List<PresenceAppointment> conflicts = appointmentRepository.findConflictingAppointments(
                    appointment.getUserId(),
                    appointment.getSubject(),
                    resourceId,
                    startOfDay,
                    endOfDay);

            // Filter the current appointment if we are updating
            if (currentAppointmentId != null) {
                conflicts = conflicts.stream()
                        .filter(a -> !a.getId().equals(currentAppointmentId))
                        .toList();
            }

            if (!conflicts.isEmpty()) {
                throw new AppointmentConflictException(
                        String.format("La persona '%s' ha già un appuntamento assegnato per la risorsa %d il giorno %s",
                                appointment.getSubject(), resourceId, appointmentDate));
            }
        }
    }

    private static final Pattern RULE_PART = Pattern.compile("([A-Z]+)=([^;]+)");
    private static final List<String> WEEKDAYS = List.of("MO", "TU", "WE", "TH", "FR");

    /**
     * Serie settimanale: FREQ=WEEKLY, UNTIL obbligatorio (dopo l'inizio, al massimo 2 mesi),
     * BYDAY solo lun-ven. Senza regola l'appuntamento è singolo.
     */
    void validateRecurrence(PresenceAppointment appointment) {
        String rule = appointment.getRecurrenceRule();
        if (rule == null || rule.isBlank()) {
            return;
        }
        Map<String, String> parts = new HashMap<>();
        Matcher m = RULE_PART.matcher(rule);
        while (m.find()) {
            parts.put(m.group(1), m.group(2));
        }
        if (!"WEEKLY".equals(parts.get("FREQ"))) {
            throw new InvalidAppointmentException("La ripetizione deve essere settimanale (FREQ=WEEKLY)");
        }
        String until = parts.get("UNTIL");
        if (until == null || until.length() < 8) {
            throw new InvalidAppointmentException("La ripetizione deve avere una data di fine (UNTIL)");
        }
        LocalDate end;
        try {
            end = LocalDate.parse(until.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidAppointmentException("Data di fine della ripetizione non valida");
        }
        LocalDate start = appointment.getStartTime().toLocalDate();
        if (!end.isAfter(start)) {
            throw new InvalidAppointmentException("La fine della ripetizione deve essere dopo il giorno di inizio");
        }
        if (end.isAfter(start.plusMonths(2))) {
            throw new InvalidAppointmentException("La ripetizione può durare al massimo 2 mesi");
        }
        String byDay = parts.get("BYDAY");
        if (byDay != null && !byDay.isBlank() && !WEEKDAYS.containsAll(List.of(byDay.split(",")))) {
            throw new InvalidAppointmentException("La ripetizione può usare solo i giorni da lunedì a venerdì");
        }
    }

    /** Presenze giorno per giorno in [from, to] (inclusi), con le serie gia' espanse. */
    public List<AppointmentOccurrence> occurrences(LocalDate from, LocalDate to) {
        List<AppointmentOccurrence> out = new ArrayList<>();
        for (PresenceAppointment a : appointmentRepository.findForOccurrences(from.atStartOfDay(),
                to.plusDays(1).atStartOfDay())) {
            out.addAll(expand(a, from, to));
        }
        return out;
    }

    static List<AppointmentOccurrence> expand(PresenceAppointment a, LocalDate from, LocalDate to) {
        List<AppointmentOccurrence> out = new ArrayList<>();
        if (a.getStartTime() == null || a.getResourceIds() == null) {
            return out;
        }
        boolean recurring = a.getRecurrenceRule() != null && !a.getRecurrenceRule().isBlank();
        for (LocalDate day : daysOf(a, from, to)) {
            for (Integer resourceId : a.getResourceIds()) {
                out.add(new AppointmentOccurrence(a.getId(), day, resourceId, a.getUserId(), a.getSubject(),
                        recurring, a.getRecurrenceRule()));
            }
        }
        return out;
    }

    /** Giorni in cui vale l'appuntamento dentro [from, to]: FREQ WEEKLY (BYDAY, INTERVAL, UNTIL) o DAILY. */
    private static List<LocalDate> daysOf(PresenceAppointment a, LocalDate from, LocalDate to) {
        LocalDate start = a.getStartTime().toLocalDate();
        List<LocalDate> days = new ArrayList<>();
        String rule = a.getRecurrenceRule();
        if (rule == null || rule.isBlank()) {
            if (!start.isBefore(from) && !start.isAfter(to)) {
                days.add(start);
            }
            return days;
        }
        Map<String, String> parts = ruleParts(rule);
        String freq = parts.getOrDefault("FREQ", "WEEKLY");
        int interval = parts.containsKey("INTERVAL") ? Math.max(1, Integer.parseInt(parts.get("INTERVAL"))) : 1;
        LocalDate last = to;
        if (parts.get("UNTIL") != null && parts.get("UNTIL").length() >= 8) {
            LocalDate until = LocalDate.parse(parts.get("UNTIL").substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
            if (until.isBefore(last)) {
                last = until;
            }
        }
        List<String> byDay = parts.get("BYDAY") == null ? List.of(weekdayCode(start.getDayOfWeek()))
                : List.of(parts.get("BYDAY").split(","));
        List<String> exceptions = a.getRecurrenceExceptions() == null ? List.of() : a.getRecurrenceExceptions();
        LocalDate weekOfStart = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (LocalDate d = from.isAfter(start) ? from : start; !d.isAfter(last); d = d.plusDays(1)) {
            boolean match = "DAILY".equals(freq)
                    ? ChronoUnit.DAYS.between(start, d) % interval == 0
                    : byDay.contains(weekdayCode(d.getDayOfWeek()))
                            && ChronoUnit.WEEKS.between(weekOfStart,
                                    d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) % interval == 0;
            if (match && !exceptions.contains(d.toString())) {
                days.add(d);
            }
        }
        return days;
    }

    private static Map<String, String> ruleParts(String rule) {
        Map<String, String> parts = new HashMap<>();
        Matcher m = RULE_PART.matcher(rule);
        while (m.find()) {
            parts.put(m.group(1), m.group(2));
        }
        return parts;
    }

    private static String weekdayCode(DayOfWeek d) {
        return d.name().substring(0, 2);
    }

    /** "Togli solo questo giorno": eccezione nella serie; un appuntamento singolo si elimina. */
    @Transactional
    public void removeOccurrence(Integer id, LocalDate date) {
        PresenceAppointment a = findById(id);
        if (a.getRecurrenceRule() == null || a.getRecurrenceRule().isBlank()) {
            appointmentRepository.deleteById(id);
            return;
        }
        List<String> ex = new ArrayList<>(a.getRecurrenceExceptions() == null ? List.of() : a.getRecurrenceExceptions());
        if (!ex.contains(date.toString())) {
            ex.add(date.toString());
        }
        a.setRecurrenceExceptions(ex);
        appointmentRepository.save(a);
    }

    /** "Termina la serie da qui": l'ultimo giorno diventa quello prima di from; se non resta nulla si elimina. */
    @Transactional
    public void endSeries(Integer id, LocalDate from) {
        PresenceAppointment a = findById(id);
        LocalDate lastDay = from.minusDays(1);
        if (a.getRecurrenceRule() == null || a.getRecurrenceRule().isBlank()
                || lastDay.isBefore(a.getStartTime().toLocalDate())) {
            appointmentRepository.deleteById(id);
            return;
        }
        String until = lastDay.format(DateTimeFormatter.BASIC_ISO_DATE);
        String rule = a.getRecurrenceRule();
        a.setRecurrenceRule(rule.contains("UNTIL=") ? rule.replaceAll("UNTIL=[^;]+", "UNTIL=" + until)
                : rule + ";UNTIL=" + until);
        appointmentRepository.save(a);
    }
}
