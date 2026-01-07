package com.p2p.realtime.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_cases", indexes = {
        @Index(name = "idx_emergency_geohash", columnList = "geohash"),
        @Index(name = "idx_emergency_user_id", columnList = "user_id"),
        @Index(name = "idx_emergency_is_resolved", columnList = "is_resolved")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyCase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID userId;

    @Column(name = "trip_id", columnDefinition = "CHAR(36)")
    private UUID tripId;

    @Column(name = "emergency_type", nullable = false)
    private String emergencyType;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String geohash;

    @Column(length = 1000)
    private String message;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private ZonedDateTime resolvedAt;

    @Column(name = "shared_with_contacts", nullable = false)
    private boolean sharedWithContacts = false;

    @Column(name = "shared_with_authorities", nullable = false)
    private boolean sharedWithAuthorities = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public enum EmergencyType {
        MEDICAL, SECURITY, ACCIDENT, OTHER
    }
}