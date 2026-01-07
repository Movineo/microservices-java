package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotNull;
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
public class LocationUpdateRequest {
    
    @NotNull
    private UUID vehicleId;
    
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    private Double speed;
    
    private Double bearing;
    
    private Double accuracy;
    
    private Double altitude;
    
    private ZonedDateTime timestamp;
    
    // Additional fields for trips
    private UUID tripId;
    
    private UUID driverId;
}
