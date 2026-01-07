package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripShareRequest {
    
    @NotNull
    private UUID tripId;
    
    @NotNull
    private UUID userId;
    
    private List<String> phoneNumbers;
    
    private Boolean generateShareCode;
}
