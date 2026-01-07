package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripTrackingRequest {
    
    @NotNull
    private UUID tripId;
    
    @NotNull
    private UUID userId;
    
    @NotNull
    private UUID vehicleId;
    
    @NotNull
    private UUID driverId;
    
    @NotNull
    private Double startLocationLat;
    
    @NotNull
    private Double startLocationLng;
    
    @NotNull
    private Double destinationLat;
    
    @NotNull
    private Double destinationLng;
    
    private Double distanceKm;
}
