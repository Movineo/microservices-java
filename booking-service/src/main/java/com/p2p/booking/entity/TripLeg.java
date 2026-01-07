package com.p2p.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_legs", indexes = {
    @Index(name = "idx_tripleg_trip_order", columnList = "trip_id, legOrder"),
    @Index(name = "idx_tripleg_status", columnList = "status"),
    @Index(name = "idx_tripleg_start_geohash", columnList = "startGeoHash"),
    @Index(name = "idx_tripleg_end_geohash", columnList = "endGeoHash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripLeg {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnore
    private Route route;
    
    @NotNull
    @Positive
    @Column(name = "leg_order", nullable = false)
    private Integer legOrder;
    
    @NotNull
    @Column(name = "start_location", nullable = false)
    private String startLocation;
    
    @NotNull
    @Column(name = "end_location", nullable = false)
    private String endLocation;
    
    @Column(name = "start_latitude", precision = 10, scale = 8)
    private BigDecimal startLatitude;
    
    @Column(name = "start_longitude", precision = 11, scale = 8)
    private BigDecimal startLongitude;
    
    @Column(name = "start_geohash", length = 20)
    private String startGeoHash;
    
    @Column(name = "end_latitude", precision = 10, scale = 8)
    private BigDecimal endLatitude;
    
    @Column(name = "end_longitude", precision = 11, scale = 8)
    private BigDecimal endLongitude;
    
    @Column(name = "end_geohash", length = 20)
    private String endGeoHash;
    
    @NotNull
    @Positive
    @Column(name = "distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm;
    
    @NotNull
    @Positive
    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;
    
    @NotNull
    @Positive
    @Column(name = "fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LegStatus status = LegStatus.PENDING;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum LegStatus {
        PENDING, IN_PROGRESS, COMPLETED, SKIPPED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}