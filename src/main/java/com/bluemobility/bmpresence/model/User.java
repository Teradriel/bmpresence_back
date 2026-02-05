package com.bluemobility.bmpresence.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "LastName", nullable = false)
    private String lastName;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "HashedPassword")
    private String hashedPassword;

    @JsonIgnore
    @Column(name = "Salt")
    private String salt;

    @Column(name = "Username")
    private String username;

    @Column(name = "AvatarUrl")
    private String avatarUrl;

    @Column(name = "Status")
    private String status;

    @Column(name = "StatusEmoji")
    private String statusEmoji;

    @Column(name = "StatusText")
    private String statusText;

    @Column(name = "IsOnline")
    private Boolean isOnline = false;

    @Column(name = "IsAdmin")
    private Boolean isAdmin = false;

    @Column(name = "Active", nullable = false)
    private Boolean active = true;

    @Column(name = "MustChangePassword")
    private Boolean mustChangePassword = false;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "LastUpdated")
    private LocalDateTime lastUpdated;

    @Column(name = "LastActiveAt")
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
