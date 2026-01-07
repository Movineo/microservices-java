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
public class SafetyAlertResponse {

    @NotNull(message = "ID cannot be null")
    private UUID id;

    @NotBlank(message = "Alert type cannot be empty")
    private String alertType;

    @NotNull(message = "Latitude cannot be null")
    private double latitude;

    @NotNull(message = "Longitude cannot be null")
    private double longitude;

    @NotBlank(message = "Geohash cannot be empty")
    private String geohash;

    @NotBlank(message = "Message cannot be empty")
    private String message;

    @NotBlank(message = "Severity cannot be empty")
    private String severity;

    @NotNull(message = "Active status cannot be null")
    private boolean active;

    @NotNull(message = "Radius cannot be null")
    private Double radiusKm;

    @NotNull(message = "Reported by cannot be null")
    private UUID reportedBy;

    @NotNull(message = "Verified status cannot be null")
    @Builder.Default
    private boolean verified = false;

    @NotNull(message = "Start time cannot be null")
    private ZonedDateTime startTime;

    private ZonedDateTime endTime;
}