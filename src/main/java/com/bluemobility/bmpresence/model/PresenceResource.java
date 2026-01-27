package com.bluemobility.bmpresence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "presence_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresenceResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String background;

    private String foreground;

    @Column(nullable = false)
    private Boolean active = true;
}
