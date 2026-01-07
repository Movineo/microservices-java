package com.p2p.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes", indexes = {
    @Index(name = "idx_route_start_geohash", columnList = "startGeoHash"),
    @Index(name = "idx_route_end_geohash", columnList = "endGeoHash"),
    @Index(name = "idx_route_provider_active", columnList = "provider_id, isActive"),
    @Index(name = "idx_route_locations", columnList = "startLocation, endLocation")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "start_location", nullable = false)
    private String startLocation;
    
    @NotBlank
    @Column(name = "end_location", nullable = false)
    private String endLocation;
    
    @Column(name = "start_latitude", precision = 10, scale = 8)
    private BigDecimal startLatitude;
    
    @Column(name = "start_longitude", precision = 11, scale = 8)
    private BigDecimal startLongitude;
    
    @Column(name = "end_latitude", precision = 10, scale = 8)
    private BigDecimal endLatitude;
    
    @Column(name = "end_longitude", precision = 11, scale = 8)
    private BigDecimal endLongitude;
    
    // GeoHash fields for efficient spatial queries
    @Column(name = "start_geohash", length = 20)
    private String startGeoHash;
    
    @Column(name = "end_geohash", length = 20)
    private String endGeoHash;
    
    // Route waypoints as comma-separated GeoHashes for intermediate points
    @Column(name = "waypoint_geohashes", columnDefinition = "TEXT")
    private String waypointGeoHashes;
    
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
    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnore
    private Provider provider;
    
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Trip> trips = new ArrayList<>();
    
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TripLeg> tripLegs = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods for waypoint management
    public List<String> getWaypointGeoHashList() {
        if (waypointGeoHashes == null || waypointGeoHashes.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(waypointGeoHashes.split(","));
    }
    
    public void setWaypointGeoHashList(List<String> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            this.waypointGeoHashes = null;
        } else {
            this.waypointGeoHashes = String.join(",", waypoints);
        }
    }
}