package com.p2p.booking.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class BookingProcessedEvent {


    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("booking_id")
    private String bookingId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("trip_id")
    private Long tripId;
    
    @JsonProperty("route_id")
    private Long routeId;
    
    @JsonProperty("vehicle_id")
    private Long vehicleId;
    
    @JsonProperty("driver_id")
    private Long driverId;
    
    @JsonProperty("provider_id")
    private Long providerId;
    
    @JsonProperty("total_fare")
    private BigDecimal totalFare;
    
    @JsonProperty("estimated_pickup_time")
    private LocalDateTime estimatedPickupTime;
    
    @JsonProperty("estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;
    
    @JsonProperty("pickup_location")
    private String pickupLocation;
    
    @JsonProperty("dropoff_location")
    private String dropoffLocation;
    
    @JsonProperty("vehicle_plate_number")
    private String vehiclePlateNumber;
    
    @JsonProperty("driver_name")
    private String driverName;
    
    @JsonProperty("driver_phone")
    private String driverPhone;
    
    @JsonProperty("provider_name")
    private String providerName;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Constructors
    public BookingProcessedEvent() {}

}