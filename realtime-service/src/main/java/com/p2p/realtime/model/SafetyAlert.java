package com.p2p.realtime.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "safety_alerts", indexes = {
        @Index(name = "idx_alert_geohash", columnList = "geohash"),
        @Index(name = "idx_alert_reported_by", columnList = "reported_by"),
        @Index(name = "idx_alert_active", columnList = "is_active"),
        @Index(name = "idx_alert_severity", columnList = "severity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyAlert {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String geohash;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String severity;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "radius_km")
    private Double radiusKm;

    @Column(name = "reported_by", columnDefinition = "CHAR(36)", nullable = false)
    private UUID reportedBy;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time")
    private ZonedDateTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = ZonedDateTime.now();
        if (startTime == null) {
            startTime = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public enum AlertType {
        ACCIDENT, ROAD_CLOSURE, TRAFFIC_JAM, WEATHER, SECURITY, OTHER
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}