package com.p2p.realtime.controller;

import com.p2p.realtime.dto.SafetyAlertRequest;
import com.p2p.realtime.dto.SafetyAlertResponse;
import com.p2p.realtime.service.SafetyAlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Slf4j
public class SafetyAlertController {

    private final SafetyAlertService safetyAlertService;

    /**
     * Create a new safety alert
     */
    @PostMapping
    public ResponseEntity<SafetyAlertResponse> createAlert(@Valid @RequestBody SafetyAlertRequest request) {
        log.info("Creating alert for type: {}", request.getAlertType());
        return ResponseEntity.ok(safetyAlertService.createAlert(request));
    }

    /**
     * Get all active alerts
     */
    @GetMapping
    public ResponseEntity<List<SafetyAlertResponse>> getAllActiveAlerts() {
        log.info("Fetching all active alerts");
        return ResponseEntity.ok(safetyAlertService.getAllActiveAlerts());
    }

    /**
     * Get alerts by geohash prefix
     */
    @GetMapping("/area/{geohashPrefix}")
    public ResponseEntity<List<SafetyAlertResponse>> getAlertsByGeohashPrefix(
            @PathVariable @NotBlank String geohashPrefix) {
        log.info("Fetching alerts for geohash prefix: {}", geohashPrefix);
        return ResponseEntity.ok(safetyAlertService.getAlertsByGeohashPrefix(geohashPrefix));
    }

    /**
     * Get alerts near a location
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<SafetyAlertResponse>> getAlertsNearLocation(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "2.0") double radiusKm) {
        log.info("Fetching alerts near location: lat={}, lon={}, radius={} km", latitude, longitude, radiusKm);
        return ResponseEntity.ok(safetyAlertService.getAlertsNearLocation(latitude, longitude, radiusKm));
    }

    /**
     * Update alert status (active/inactive)
     */
    @PutMapping("/{alertId}/status")
    public ResponseEntity<SafetyAlertResponse> updateAlertStatus(
            @PathVariable UUID alertId,
            @RequestParam boolean active) {
        log.info("Updating status of alert {} to active={}", alertId, active);
        return ResponseEntity.ok(safetyAlertService.updateAlertStatus(alertId, active));
    }

    /**
     * Verify an alert
     */
    @PutMapping("/{alertId}/verify")
    public ResponseEntity<SafetyAlertResponse> verifyAlert(@PathVariable UUID alertId) {
        log.info("Verifying alert {}", alertId);
        return ResponseEntity.ok(safetyAlertService.verifyAlert(alertId));
    }

    /**
     * Get active alerts reported by a specific user
     */
    @GetMapping("/reported-by/{reportedBy}")
    public ResponseEntity<List<SafetyAlertResponse>> getAlertsByReportedBy(
            @PathVariable UUID reportedBy) {
        log.info("Fetching active alerts reported by user: {}", reportedBy);
        return ResponseEntity.ok(safetyAlertService.getAlertsByReportedBy(reportedBy));
    }

    /**
     * Get active alerts by alert type
     */
    @GetMapping("/type/{alertType}")
    public ResponseEntity<List<SafetyAlertResponse>> getAlertsByAlertType(
            @PathVariable @NotBlank @Pattern(regexp = "ACCIDENT|ROAD_CLOSURE|TRAFFIC_JAM|WEATHER|SECURITY|OTHER",
                    message = "Invalid alert type") String alertType) {
        log.info("Fetching active alerts of type: {}", alertType);
        return ResponseEntity.ok(safetyAlertService.getAlertsByAlertType(alertType));
    }
}