package com.bluemobility.bmpresence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "presence_appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresenceAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String subject;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "recurrence_rule")
    private String recurrenceRule;

    @Column(nullable = false)
    private Boolean active = true;

    @ElementCollection
    @CollectionTable(name = "appointment_resources", joinColumns = @JoinColumn(name = "appointment_id"))
    @Column(name = "resource_id")
    private List<Integer> resourceIds;
}
