package com.bluemobility.bmpresence.dto;

import java.time.LocalDate;

/**
 * Una presenza in un giorno preciso (le serie sono gia' espanse dal server).
 * {@code appointmentId} e' l'appuntamento o la serie a cui appartiene.
 */
public record AppointmentOccurrence(
        Integer appointmentId,
        LocalDate date,
        Integer resourceId,
        Integer userId,
        String subject,
        boolean recurring,
        String recurrenceRule) {
}
