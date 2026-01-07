package com.p2p.realtime.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyRequest {

    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    private UUID tripId;

    @NotBlank(message = "Emergency type cannot be empty")
    @Pattern(regexp = "MEDICAL|SECURITY|ACCIDENT|OTHER", message = "Emergency type must be one of: MEDICAL, SECURITY, ACCIDENT, OTHER")
    private String emergencyType;

    @NotNull(message = "Latitude cannot be null")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private double latitude;

    @NotNull(message = "Longitude cannot be null")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private double longitude;

    private String message;

    @Builder.Default
    private boolean shareWithContacts = false;

    @Builder.Default
    private boolean shareWithAuthorities = false;
}