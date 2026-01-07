package com.p2p.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripTrackingResponse {
    
    private UUID tripId;
    
    private UUID userId;
    
    private UUID vehicleId;
    
    private UUID driverId;
    
    private double startLocationLat;
    
    private double startLocationLng;
    
    private double destinationLat;
    
    private double destinationLng;
    
    private Double currentLocationLat;
    
    private Double currentLocationLng;
    
    private ZonedDateTime startTime;
    
    private ZonedDateTime estimatedArrivalTime;
    
    private String status;
    
    private Double distanceKm;
    
    private Double completedDistanceKm;
    
    private Double remainingDistanceKm;
    
    private Integer estimatedMinutesRemaining;
    
    private String shareCode;
    
    private Boolean isShared;
}
