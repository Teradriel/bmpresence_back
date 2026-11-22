package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.dto.AppointmentOccurrence;
import com.bluemobility.bmpresence.model.PresenceAppointment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentOccurrencesTest {

    private PresenceAppointment series(String rule, List<String> exceptions) {
        PresenceAppointment a = new PresenceAppointment();
        a.setId(7);
        a.setUserId(3);
        a.setSubject("Luca Moretti");
        a.setStartTime(LocalDateTime.of(2026, 11, 16, 8, 30)); // lunedi
        a.setResourceIds(List.of(1));
        a.setRecurrenceRule(rule);
        a.setRecurrenceExceptions(exceptions);
        return a;
    }

    private List<String> days(PresenceAppointment a, String from, String to) {
        return PresenceAppointmentService.expand(a, LocalDate.parse(from), LocalDate.parse(to)).stream()
                .map(AppointmentOccurrence::date).map(LocalDate::toString).toList();
    }

    @Test
    void singleAppointmentOnlyInsideRange() {
        PresenceAppointment a = series(null, null);
        assertEquals(List.of("2026-11-16"), days(a, "2026-11-16", "2026-11-20"));
        assertEquals(List.of(), days(a, "2026-11-23", "2026-11-27"));
    }

    @Test
    void weeklySeriesWithDaysUntilAndExceptions() {
        PresenceAppointment a = series("FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20261202", List.of("2026-11-23"));
        assertEquals(List.of("2026-11-16", "2026-11-18", "2026-11-25", "2026-11-30", "2026-12-02"),
                days(a, "2026-11-16", "2026-12-11"));
        // range che inizia a meta' serie
        assertEquals(List.of("2026-11-25"), days(a, "2026-11-24", "2026-11-27"));
    }

    @Test
    void weeklyDefaultsToStartWeekdayAndHonorsInterval() {
        PresenceAppointment a = series("FREQ=WEEKLY;INTERVAL=2", null);
        assertEquals(List.of("2026-11-16", "2026-11-30"), days(a, "2026-11-16", "2026-12-04"));
    }
}
