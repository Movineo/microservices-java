package com.p2p.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripShareResponse {
    
    private UUID tripId;
    
    private Boolean isShared;
    
    private String shareCode;
    
    private String shareUrl;
}
