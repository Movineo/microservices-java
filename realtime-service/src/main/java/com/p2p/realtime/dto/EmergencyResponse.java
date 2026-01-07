package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotBlank;
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
public class EmergencyResponse {

    @NotNull
    private UUID id;

    @NotNull
    private UUID userId;

    private UUID tripId;

    @NotBlank
    private String emergencyType;

    @NotNull
    private double latitude;

    @NotNull
    private double longitude;

    @NotBlank
    private String geohash;

    private String message;

    @NotNull
    private boolean resolved;

    private ZonedDateTime resolvedAt;

    @NotNull
    private boolean sharedWithContacts;

    @NotNull
    private boolean sharedWithAuthorities;

    @NotNull
    private ZonedDateTime createdAt;
}