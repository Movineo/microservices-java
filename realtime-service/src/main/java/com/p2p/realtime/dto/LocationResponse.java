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
public class LocationResponse {
    
    private UUID vehicleId;
    
    private double latitude;
    
    private double longitude;
    
    private String geohash;
    
    private Double speed;
    
    private Double bearing;
    
    private ZonedDateTime timestamp;
}
