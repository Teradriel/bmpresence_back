package com.bluemobility.bmpresence.repository;

import com.bluemobility.bmpresence.model.PresenceAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PresenceAppointmentRepository extends JpaRepository<PresenceAppointment, Integer> {

        List<PresenceAppointment> findByActiveTrue();

        @Query("SELECT a FROM PresenceAppointment a WHERE a.active = true AND a.startTime >= :start AND a.endTime <= :end")
        List<PresenceAppointment> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        /** Appuntamenti singoli dentro [start, end) e tutte le serie iniziate prima di end. */
        @Query("SELECT a FROM PresenceAppointment a WHERE a.active = true AND a.startTime < :end AND "
                        + "((a.recurrenceRule IS NULL OR a.recurrenceRule = '') AND a.startTime >= :start "
                        + "OR a.recurrenceRule IS NOT NULL AND a.recurrenceRule <> '')")
        List<PresenceAppointment> findForOccurrences(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query(value = "SELECT * FROM PresenceAppointments a WHERE JSON_CONTAINS(a.ResourceIds, CAST(:resourceId AS JSON), '$') AND a.Active = true", nativeQuery = true)
        List<PresenceAppointment> findByResourceId(@Param("resourceId") Integer resourceId);

        @Query(value = "SELECT * FROM PresenceAppointments a WHERE " +
                        "((:userId IS NOT NULL AND a.UserId = :userId) OR (:userId IS NULL AND a.Subject = :subject)) AND JSON_CONTAINS(a.ResourceIds, CAST(:resourceId AS JSON), '$') AND a.Active = true AND "
                        +
                        "((a.StartTime >= :startOfDay AND a.StartTime < :endOfDay) OR " +
                        "(a.EndTime > :startOfDay AND a.EndTime <= :endOfDay))", nativeQuery = true)
        List<PresenceAppointment> findConflictingAppointments(
                        @Param("userId") Integer userId,
                        @Param("subject") String subject,
                        @Param("resourceId") Integer resourceId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay);
}
