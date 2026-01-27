package com.bluemobility.bmpresence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hashed_password")
    private String hashedPassword;

    private String salt;

    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String status;

    @Column(name = "status_emoji")
    private String statusEmoji;

    @Column(name = "status_text")
    private String statusText;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    @Transient
    public String getFullName() {
        return (name + " " + lastName).trim();
    }
}
