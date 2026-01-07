package com.p2p.realtime.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_tracking", indexes = {
        @Index(name = "idx_trip_geohash", columnList = "current_location_geohash"),
        @Index(name = "idx_trip_trip_id", columnList = "trip_id"),
        @Index(name = "idx_trip_user_id", columnList = "user_id"),
        @Index(name = "idx_trip_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripTracking {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "trip_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID tripId;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID userId;

    @Column(name = "vehicle_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID vehicleId;

    @Column(name = "driver_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID driverId;

    @Column(name = "start_location_lat", nullable = false)
    private double startLocationLat;

    @Column(name = "start_location_lng", nullable = false)
    private double startLocationLng;

    @Column(name = "start_location_geohash", nullable = false)
    private String startLocationGeohash;

    @Column(name = "destination_lat", nullable = false)
    private double destinationLat;

    @Column(name = "destination_lng", nullable = false)
    private double destinationLng;

    @Column(name = "destination_geohash", nullable = false)
    private String destinationGeohash;

    @Column(name = "current_location_lat")
    private Double currentLocationLat;

    @Column(name = "current_location_lng")
    private Double currentLocationLng;

    @Column(name = "current_location_geohash")
    private String currentLocationGeohash;

    @Column(name = "start_time")
    private ZonedDateTime startTime;

    @Column(name = "estimated_arrival_time")
    private ZonedDateTime estimatedArrivalTime;

    @Column(nullable = false)
    private String status;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "share_code")
    private String shareCode;

    @Column(name = "is_shared", nullable = false)
    private boolean isShared = false;

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

    public enum TripStatus {
        WAITING, STARTED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}