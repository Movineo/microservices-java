package com.p2p.realtime.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_locations", indexes = {
        @Index(name = "idx_vehicle_location_geohash", columnList = "geohash"),
        @Index(name = "idx_vehicle_location_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_vehicle_location_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLocation {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID vehicleId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String geohash;

    @Column
    private Double speed;

    @Column
    private Double bearing;

    @Column
    private Double accuracy;

    @Column
    private Double altitude;

    @Column(nullable = false)
    private ZonedDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = timestamp = ZonedDateTime.now();
    }
}