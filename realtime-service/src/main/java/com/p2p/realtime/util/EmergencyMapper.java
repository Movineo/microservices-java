package com.p2p.realtime.util;

import com.p2p.realtime.dto.EmergencyResponse;
import com.p2p.realtime.dto.SafetyAlertResponse;
import com.p2p.realtime.model.EmergencyCase;
import com.p2p.realtime.model.SafetyAlert;

public class EmergencyMapper {

    /**
     * Convert EmergencyCase entity to EmergencyResponse DTO
     */
    public static EmergencyResponse buildEmergencyResponse(EmergencyCase emergency) {
        return EmergencyResponse.builder()
                .id(emergency.getId())
                .userId(emergency.getUserId())
                .tripId(emergency.getTripId())
                .emergencyType(emergency.getEmergencyType())
                .latitude(emergency.getLatitude())
                .longitude(emergency.getLongitude())
                .geohash(emergency.getGeohash())
                .message(emergency.getMessage())
                .resolved(emergency.isResolved())
                .resolvedAt(emergency.getResolvedAt())
                .sharedWithContacts(emergency.isSharedWithContacts())
                .sharedWithAuthorities(emergency.isSharedWithAuthorities())
                .createdAt(emergency.getCreatedAt())
                .build();
    }

    /**
     * Convert SafetyAlert entity to SafetyAlertResponse DTO
     */
    public static SafetyAlertResponse buildSafetyAlertResponse(SafetyAlert alert) {
        return SafetyAlertResponse.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .geohash(alert.getGeohash())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .active(alert.isActive())
                .radiusKm(alert.getRadiusKm())
                .reportedBy(alert.getReportedBy())
                .verified(alert.isVerified())
                .startTime(alert.getStartTime())
                .endTime(alert.getEndTime())
                .build();
    }
}