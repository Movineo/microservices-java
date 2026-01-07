package com.p2p.booking.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class TripCompletedEvent {

    // Getters and Setters
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("trip_id")
    private Long tripId;
    
    @JsonProperty("booking_id")
    private String bookingId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("driver_id")
    private Long driverId;
    
    @JsonProperty("vehicle_id")
    private Long vehicleId;
    
    @JsonProperty("provider_id")
    private Long providerId;
    
    @JsonProperty("total_fare")
    private BigDecimal totalFare;
    
    @JsonProperty("actual_distance_km")
    private BigDecimal actualDistanceKm;
    
    @JsonProperty("actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @JsonProperty("pickup_location")
    private String pickupLocation;
    
    @JsonProperty("dropoff_location")
    private String dropoffLocation;
    
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    
    @JsonProperty("completed_at")
    private LocalDateTime completedAt;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public TripCompletedEvent(String eventType, Long tripId, String bookingId,
                             String userId, String phoneNumber, Long driverId, 
                             Long vehicleId, Long providerId, BigDecimal totalFare) {
        this.eventType = eventType;
        this.tripId = tripId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.providerId = providerId;
        this.totalFare = totalFare;
        this.completedAt = LocalDateTime.now();
        this.timestamp = LocalDateTime.now();
    }

}