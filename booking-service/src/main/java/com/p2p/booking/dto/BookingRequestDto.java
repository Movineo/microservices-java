package com.p2p.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class BookingRequestDto {
    
    @NotBlank(message = "Start location is required")
    private String startLocation;
    
    @NotBlank(message = "End location is required")
    private String endLocation;
    
    private BigDecimal startLatitude;
    private BigDecimal startLongitude;
    private BigDecimal endLatitude;
    private BigDecimal endLongitude;
    
    @NotNull(message = "Preferred date and time is required")
    private LocalDateTime preferredDateTime;
    
    @Positive(message = "Passenger count must be positive")
    @Builder.Default
    private Integer passengerCount = 1;
    
    private String providerType;
    private String specialRequests;
}