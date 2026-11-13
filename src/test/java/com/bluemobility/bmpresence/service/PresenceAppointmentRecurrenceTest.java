package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.exception.InvalidAppointmentException;
import com.bluemobility.bmpresence.model.PresenceAppointment;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresenceAppointmentRecurrenceTest {

    private final PresenceAppointmentService service = new PresenceAppointmentService(null);

    private PresenceAppointment on(String rule) {
        PresenceAppointment a = new PresenceAppointment();
        a.setStartTime(LocalDateTime.of(2026, 11, 16, 8, 30)); // lunedì
        a.setRecurrenceRule(rule);
        return a;
    }

    @Test
    void singleAppointmentAndValidSeriesPass() {
        assertDoesNotThrow(() -> service.validateRecurrence(on(null)));
        assertDoesNotThrow(() -> service.validateRecurrence(on("FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20270116")));
        assertDoesNotThrow(() -> service.validateRecurrence(on("FREQ=WEEKLY;UNTIL=20270116T000000Z")));
    }

    @Test
    void invalidSeriesAreRejected() {
        for (String rule : new String[] {
                "FREQ=WEEKLY;BYDAY=MO", // senza fine
                "FREQ=DAILY;UNTIL=20261230", // non settimanale
                "FREQ=WEEKLY;UNTIL=20261116", // fine = inizio
                "FREQ=WEEKLY;UNTIL=20270117", // oltre 2 mesi
                "FREQ=WEEKLY;BYDAY=SA;UNTIL=20261230", // sabato
                "FREQ=WEEKLY;UNTIL=abcdefgh" }) {
            assertThrows(InvalidAppointmentException.class, () -> service.validateRecurrence(on(rule)), rule);
        }
    }
}
