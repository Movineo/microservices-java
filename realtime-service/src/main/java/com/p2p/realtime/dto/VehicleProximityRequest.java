package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleProximityRequest {
    
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    private Double radiusKm;
    
    private String vehicleType;
    
    private Integer limit;
}
