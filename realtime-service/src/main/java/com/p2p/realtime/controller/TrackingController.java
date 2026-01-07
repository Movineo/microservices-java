package com.p2p.realtime.controller;

import com.p2p.realtime.dto.*;
import com.p2p.realtime.service.TrackingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * Update vehicle location
     */
    @PostMapping("/location")
    public ResponseEntity<LocationResponse> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        log.info("Updating location for vehicle: {}", request.getVehicleId());
        return ResponseEntity.ok(trackingService.updateLocation(request));
    }

    /**
     * WebSocket endpoint for location updates
     */
    @MessageMapping("/location/update")
    @SendTo("/topic/vehicle/locations")
    public WebSocketMessage<LocationResponse> handleLocationUpdateWs(LocationUpdateRequest request) {
        log.info("Received WebSocket location update for vehicle: {}", request.getVehicleId());
        LocationResponse response = trackingService.updateLocation(request);
        return WebSocketMessage.create(WebSocketMessage.MessageType.LOCATION_UPDATE.name(), response);
    }

    /**
     * Start tracking a trip
     */
    @PostMapping("/trips")
    public ResponseEntity<TripTrackingResponse> startTrip(@Valid @RequestBody TripTrackingRequest request) {
        log.info("Starting trip for user: {}, tripId: {}", request.getUserId(), request.getTripId());
        return ResponseEntity.ok(trackingService.startTrip(request));
    }

    /**
     * Get trip details
     */
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<TripTrackingResponse> getTripById(@PathVariable UUID tripId) {
        log.info("Fetching trip details for tripId: {}", tripId);
        return ResponseEntity.ok(trackingService.getTripById(tripId));
    }

    /**
     * Get a trip using share code
     */
    @GetMapping("/shared/{shareCode}")
    public ResponseEntity<TripTrackingResponse> getSharedTrip(@PathVariable @NotBlank String shareCode) {
        log.info("Fetching shared trip for shareCode: {}", shareCode);
        return ResponseEntity.ok(trackingService.getSharedTrip(shareCode));
    }

    /**
     * Get all active trips for a user
     */
    @GetMapping("/trips/user/{userId}")
    public ResponseEntity<List<TripTrackingResponse>> getUserActiveTrips(@PathVariable UUID userId) {
        log.info("Fetching active trips for user: {}", userId);
        return ResponseEntity.ok(trackingService.getUserActiveTrips(userId));
    }

    /**
     * Get all trips for a user
     */
    @GetMapping("/trips/user/{userId}/all")
    public ResponseEntity<List<TripTrackingResponse>> getAllTripsByUserId(@PathVariable UUID userId) {
        log.info("Fetching all trips for user: {}", userId);
        return ResponseEntity.ok(trackingService.getAllTripsByUserId(userId));
    }

    /**
     * Get all trips for a vehicle with a specific status
     */
    @GetMapping("/trips/vehicle/{vehicleId}/status/{status}")
    public ResponseEntity<List<TripTrackingResponse>> getTripsByVehicleIdAndStatus(
            @PathVariable UUID vehicleId,
            @PathVariable @NotBlank @Pattern(regexp = "WAITING|STARTED|IN_PROGRESS|COMPLETED|CANCELLED",
                    message = "Invalid trip status") String status) {
        log.info("Fetching trips for vehicle: {} with status: {}", vehicleId, status);
        return ResponseEntity.ok(trackingService.getTripsByVehicleIdAndStatus(vehicleId, status));
    }

    /**
     * Get all trips for a driver with a specific status
     */
    @GetMapping("/trips/driver/{driverId}/status/{status}")
    public ResponseEntity<List<TripTrackingResponse>> getTripsByDriverIdAndStatus(
            @PathVariable UUID driverId,
            @PathVariable @NotBlank @Pattern(regexp = "WAITING|STARTED|IN_PROGRESS|COMPLETED|CANCELLED",
                    message = "Invalid trip status") String status) {
        log.info("Fetching trips for driver: {} with status: {}", driverId, status);
        return ResponseEntity.ok(trackingService.getTripsByDriverIdAndStatus(driverId, status));
    }

    /**
     * Get all trips with a specific status
     */
    @GetMapping("/trips/status/{status}")
    public ResponseEntity<List<TripTrackingResponse>> getTripsByStatus(
            @PathVariable @NotBlank @Pattern(regexp = "WAITING|STARTED|IN_PROGRESS|COMPLETED|CANCELLED",
                    message = "Invalid trip status") String status) {
        log.info("Fetching trips with status: {}", status);
        return ResponseEntity.ok(trackingService.getTripsByStatus(status));
    }

    /**
     * Update trip status
     */
    @PutMapping("/trips/{tripId}/status/{status}")
    public ResponseEntity<TripTrackingResponse> updateTripStatus(
            @PathVariable UUID tripId,
            @PathVariable @NotBlank @Pattern(regexp = "WAITING|STARTED|IN_PROGRESS|COMPLETED|CANCELLED",
                    message = "Invalid trip status") String status) {
        log.info("Updating status of trip {} to {}", tripId, status);
        return ResponseEntity.ok(trackingService.updateTripStatus(tripId, status));
    }

    /**
     * Share a trip
     */
    @PostMapping("/trips/share")
    public ResponseEntity<TripShareResponse> shareTrip(@Valid @RequestBody TripShareRequest request) {
        log.info("Sharing trip for tripId: {}, userId: {}", request.getTripId(), request.getUserId());
        return ResponseEntity.ok(trackingService.shareTrip(request));
    }

    /**
     * Find vehicles near a location
     */
    @PostMapping("/vehicles/nearby")
    public ResponseEntity<List<LocationResponse>> findVehiclesNear(@Valid @RequestBody VehicleProximityRequest request) {
        log.info("Finding vehicles near location: lat={}, lon={}, radius={} km",
                request.getLatitude(), request.getLongitude(), request.getRadiusKm());
        return ResponseEntity.ok(trackingService.findVehiclesNear(request));
    }

    /**
     * Get the latest location for a vehicle
     */
    @GetMapping("/vehicles/{vehicleId}/location")
    public ResponseEntity<LocationResponse> getLatestVehicleLocation(@PathVariable UUID vehicleId) {
        log.info("Fetching latest location for vehicle: {}", vehicleId);
        return ResponseEntity.ok(trackingService.getLatestVehicleLocation(vehicleId));
    }

    /**
     * Get location history for a vehicle within a time range
     */
    @GetMapping("/vehicles/{vehicleId}/history")
    public ResponseEntity<List<LocationResponse>> getVehicleLocationHistory(
            @PathVariable UUID vehicleId,
            @RequestParam @NotBlank String startTime,
            @RequestParam @NotBlank String endTime) {
        log.info("Fetching location history for vehicle: {} from {} to {}", vehicleId, startTime, endTime);
        ZonedDateTime start = ZonedDateTime.parse(startTime);
        ZonedDateTime end = ZonedDateTime.parse(endTime);
        return ResponseEntity.ok(trackingService.getVehicleLocationHistory(vehicleId, start, end));
    }
}