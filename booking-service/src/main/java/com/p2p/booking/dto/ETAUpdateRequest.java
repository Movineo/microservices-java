package com.p2p.booking.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ETAUpdateRequest {
    private BigDecimal currentLat;
    private BigDecimal currentLon;
    private BigDecimal destinationLat;
    private BigDecimal destinationLon;
}
