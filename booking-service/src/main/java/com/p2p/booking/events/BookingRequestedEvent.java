package com.p2p.booking.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class BookingRequestedEvent {

    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("booking_id")
    private String bookingId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("start_point")
    private String startPoint;
    
    @JsonProperty("end_point")
    private String endPoint;
    
    @JsonProperty("travel_date")
    private String travelDate;
    
    @JsonProperty("preferred_time")
    private String preferredTime;
    
    @JsonProperty("passenger_count")
    private Integer passengerCount;
    
    @JsonProperty("provider_type")
    private String providerType;
    
    @JsonProperty("special_requests")
    private String specialRequests;
    
    @JsonProperty("pickup_latitude")
    private Double pickupLatitude;
    
    @JsonProperty("pickup_longitude")
    private Double pickupLongitude;
    
    @JsonProperty("dropoff_latitude")
    private Double dropoffLatitude;
    
    @JsonProperty("dropoff_longitude")
    private Double dropoffLongitude;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Constructors
    public BookingRequestedEvent() {}

}