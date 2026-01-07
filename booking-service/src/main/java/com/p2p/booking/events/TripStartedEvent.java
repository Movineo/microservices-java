package com.p2p.booking.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class TripStartedEvent {


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
    
    @JsonProperty("pickup_location")
    private String pickupLocation;
    
    @JsonProperty("dropoff_location")
    private String dropoffLocation;
    
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public TripStartedEvent(String eventType, Long tripId, String bookingId,
                           String userId, String phoneNumber, Long driverId, 
                           Long vehicleId, String pickupLocation, String dropoffLocation) {
        this.eventType = eventType;
        this.tripId = tripId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.startedAt = LocalDateTime.now();
        this.timestamp = LocalDateTime.now();
    }

}