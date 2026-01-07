package com.p2p.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {
    
    private Long tripId;
    private String bookingId;
    private String status;
    private BigDecimal totalFare;
    
    private String pickupLocation;
    private String dropoffLocation;
    private LocalDateTime estimatedPickupTime;
    private LocalDateTime estimatedArrivalTime;
    
    // Vehicle Information
    private Long vehicleId;
    private String vehiclePlateNumber;
    private String vehicleModel;
    private String vehicleMake;
    
    // Driver Information
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private BigDecimal driverRating;
    
    // Provider Information
    private Long providerId;
    private String providerName;
    private String providerType;
    private BigDecimal providerRating;
    
    // Route Information
    private Long routeId;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    
    private String message;
    private LocalDateTime createdAt;
}