package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyAlertRequest {

    @NotBlank(message = "Alert type cannot be empty")
    private String alertType;

    @NotNull(message = "Latitude cannot be null")
    private double latitude;

    @NotNull(message = "Longitude cannot be null")
    private double longitude;

    @NotBlank(message = "Message cannot be empty")
    private String message;

    @NotBlank(message = "Severity cannot be empty")
    private String severity;

    @NotNull(message = "Radius cannot be null")
    @Builder.Default
    private Double radiusKm = 5.0;

    @NotNull(message = "Reported by cannot be null")
    private UUID reportedBy;

    @NotNull(message = "Start time cannot be null")
    @Builder.Default
    private ZonedDateTime startTime = ZonedDateTime.now();

    private ZonedDateTime endTime;
}