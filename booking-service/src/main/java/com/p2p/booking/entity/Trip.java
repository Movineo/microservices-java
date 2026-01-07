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
@Table(name = "trips", indexes = {
    @Index(name = "idx_trip_booking_id", columnList = "bookingId", unique = true),
    @Index(name = "idx_trip_user_id", columnList = "userId"),
    @Index(name = "idx_trip_status", columnList = "status"),
    @Index(name = "idx_trip_pickup_geohash", columnList = "pickupGeoHash"),
    @Index(name = "idx_trip_dropoff_geohash", columnList = "dropoffGeoHash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "booking_id", nullable = false, unique = true)
    private String bookingId;
    
    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotBlank
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnore
    private Route route;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIgnore
    private Driver driver;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.PENDING;
    
    @NotNull
    @Positive
    @Column(name = "total_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;
    
    @Column(name = "passenger_count")
    @Builder.Default
    private Integer passengerCount = 1;
    
    @Column(name = "pickup_location")
    private String pickupLocation;
    
    @Column(name = "pickup_latitude", precision = 10, scale = 8)
    private BigDecimal pickupLatitude;
    
    @Column(name = "pickup_longitude", precision = 11, scale = 8)
    private BigDecimal pickupLongitude;
    
    @Column(name = "pickup_geohash", length = 20)
    private String pickupGeoHash;
    
    @Column(name = "dropoff_location")
    private String dropoffLocation;
    
    @Column(name = "dropoff_latitude", precision = 10, scale = 8)
    private BigDecimal dropoffLatitude;
    
    @Column(name = "dropoff_longitude", precision = 11, scale = 8)
    private BigDecimal dropoffLongitude;
    
    @Column(name = "dropoff_geohash", length = 20)
    private String dropoffGeoHash;
    
    @Column(name = "scheduled_pickup_time")
    private LocalDateTime scheduledPickupTime;
    
    @Column(name = "actual_pickup_time")
    private LocalDateTime actualPickupTime;
    
    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;
    
    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;
    
    @Column(name = "special_requests")
    private String specialRequests;
    
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TripLeg> tripLegs = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TripStatus {
        PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
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