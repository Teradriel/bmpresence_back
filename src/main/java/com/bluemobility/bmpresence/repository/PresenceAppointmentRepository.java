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

    @Query("SELECT a FROM PresenceAppointment a JOIN a.resourceIds r WHERE r = :resourceId AND a.active = true")
    List<PresenceAppointment> findByResourceId(@Param("resourceId") Integer resourceId);
}
