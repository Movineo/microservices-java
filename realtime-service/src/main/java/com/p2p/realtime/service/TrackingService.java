package com.p2p.realtime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.realtime.dto.*;
import com.p2p.realtime.util.TrackingMapper;
import com.p2p.realtime.model.TripTracking;
import com.p2p.realtime.model.VehicleLocation;
import com.p2p.realtime.repository.TripTrackingRepository;
import com.p2p.realtime.repository.VehicleLocationRepository;
import com.p2p.realtime.util.GeoHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final VehicleLocationRepository vehicleLocationRepository;
    private final TripTrackingRepository tripTrackingRepository;
    private final GeoHashUtil geoHashUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TrackingMapper trackingMapper;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.trip-events}")
    private String tripEventsTopic;

    @Value("${kafka.topics.vehicle-location-updates}")
    private String vehicleLocationUpdatesTopic;

    @Value("${realtime.cache.vehicle-locations-ttl-seconds}")
    private long vehicleLocationsTtl;

    private static final SecureRandom RANDOM = new SecureRandom();
    // Character set for generating share codes (ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789)
    @SuppressWarnings("SpellCheckingInspection")
    private static final String SHARE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHARE_CODE_LENGTH = 6;

    /**
     * Generate a random share code for trip sharing
     */
    private String generateShareCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SHARE_CODE_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(SHARE_CODE_CHARS.length());
            sb.append(SHARE_CODE_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * Update vehicle location and related trip if it exists
     */
    @Transactional
    public LocationResponse updateLocation(LocationUpdateRequest request) {
        // Generate geohash for the location
        String geohash = geoHashUtil.encode(request.getLatitude(), request.getLongitude());

        // Create a new location entity
        VehicleLocation location = VehicleLocation.builder()
                .vehicleId(request.getVehicleId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geohash(geohash)
                .speed(request.getSpeed())
                .bearing(request.getBearing())
                .accuracy(request.getAccuracy())
                .altitude(request.getAltitude())
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : ZonedDateTime.now())
                .build();

        // Save to the database
        vehicleLocationRepository.save(location);

        // Cache the latest location
        String cacheKey = "vehicle:location:" + request.getVehicleId();
        redisTemplate.opsForValue().set(cacheKey, location, vehicleLocationsTtl, TimeUnit.SECONDS);

        // Publish location update to Kafka
        try {
            String locationJson = objectMapper.writeValueAsString(request);
            kafkaTemplate.send(vehicleLocationUpdatesTopic, request.getVehicleId().toString(), locationJson);
            log.info("Published vehicle location update for vehicleId: {}", request.getVehicleId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize location update for vehicleId: {}", request.getVehicleId(), e);
        }

        // Update trip tracking if tripId is provided
        if (request.getTripId() != null) {
            updateTripLocation(request);
        }

        // Build response
        LocationResponse response = trackingMapper.toLocationResponse(location);

        // Notify subscribers via WebSocket
        messagingTemplate.convertAndSend("/topic/vehicle/" + request.getVehicleId(),
                WebSocketMessage.create(WebSocketMessage.MessageType.LOCATION_UPDATE.name(), response));

        return response;
    }

    /**
     * Update the trip with current location information
     */
    private void updateTripLocation(LocationUpdateRequest request) {
        tripTrackingRepository.findByTripId(request.getTripId()).ifPresent(trip -> {
            // Update current location
            trip.setCurrentLocationLat(request.getLatitude());
            trip.setCurrentLocationLng(request.getLongitude());
            trip.setCurrentLocationGeohash(geoHashUtil.encode(request.getLatitude(), request.getLongitude()));

            // Calculate remaining distance
            double remainingDistance = geoHashUtil.calculateDistance(
                    request.getLatitude(),
                    request.getLongitude(),
                    trip.getDestinationLat(),
                    trip.getDestinationLng()
            );

            // Calculate ETA if speed is available
            if (request.getSpeed() != null && request.getSpeed() > 0) {
                int etaMinutes = geoHashUtil.calculateETA(remainingDistance, request.getSpeed());
                trip.setEstimatedArrivalTime(ZonedDateTime.now().plusMinutes(etaMinutes));
            }

            // Save and notify
            updateAndNotifyTrip(trip);
        });
    }

    /**
     * Update trip with latest vehicle location and save
     */
    private void updateTripWithLatestLocation(TripTracking trip) {
        VehicleLocation latestLocation = vehicleLocationRepository.findTopByVehicleIdOrderByTimestampDesc(trip.getVehicleId());
        if (latestLocation != null) {
            trip.setCurrentLocationLat(latestLocation.getLatitude());
            trip.setCurrentLocationLng(latestLocation.getLongitude());
            trip.setCurrentLocationGeohash(latestLocation.getGeohash());
            tripTrackingRepository.save(trip);
        }
    }

    /**
     * Save trip, cache, and notify subscribers
     */
    private void updateAndNotifyTrip(TripTracking trip) {
        // Save the updated trip
        TripTracking updatedTrip = tripTrackingRepository.save(trip);

        // Cache the trip status
        String cacheKey = "trip:tracking:" + trip.getTripId();
        redisTemplate.opsForValue().set(cacheKey, updatedTrip, vehicleLocationsTtl, TimeUnit.SECONDS);

        // Notify trip subscribers via WebSocket
        TripTrackingResponse tripResponse = trackingMapper.toTripTrackingResponse(updatedTrip);
        messagingTemplate.convertAndSend("/topic/trip/" + trip.getTripId(),
                WebSocketMessage.create(WebSocketMessage.MessageType.TRIP_UPDATE.name(), tripResponse));

        // Send individual update to the user
        messagingTemplate.convertAndSendToUser(trip.getUserId().toString(),
                "/queue/trips", WebSocketMessage.create(WebSocketMessage.MessageType.TRIP_UPDATE.name(), tripResponse));
    }

    /**
     * Start tracking a new trip
     */
    @Transactional
    public TripTrackingResponse startTrip(TripTrackingRequest request) {
        // Check if the trip already exists
        Optional<TripTracking> existingTrip = tripTrackingRepository.findByTripId(request.getTripId());

        if (existingTrip.isPresent()) {
            // Trip already exists, update status to STARTED
            TripTracking trip = existingTrip.get();
            trip.setStatus(TripTracking.TripStatus.STARTED.name());
            trip.setStartTime(ZonedDateTime.now());

            // Update with latest vehicle location
            updateTripWithLatestLocation(trip);

            // Save and notify
            TripTracking updatedTrip = tripTrackingRepository.save(trip);
            TripTrackingResponse response = trackingMapper.toTripTrackingResponse(updatedTrip);

            // Publish trip start event to Kafka
            kafkaTemplate.send(tripEventsTopic, updatedTrip.getTripId().toString(), "TRIP_STARTED");
            log.info("Published trip start event for tripId: {}", updatedTrip.getTripId());

            // Notify subscribers via WebSocket
            messagingTemplate.convertAndSend("/topic/trip/" + updatedTrip.getTripId(),
                    WebSocketMessage.create(WebSocketMessage.MessageType.TRIP_UPDATE.name(), response));

            return response;
        }

        // Generate geohashes
        String startLocationGeohash = geoHashUtil.encode(request.getStartLocationLat(), request.getStartLocationLng());
        String destinationGeohash = geoHashUtil.encode(request.getDestinationLat(), request.getDestinationLng());

        // Calculate distance if not provided
        Double distance = request.getDistanceKm();
        if (distance == null) {
            distance = geoHashUtil.calculateDistance(
                    request.getStartLocationLat(),
                    request.getStartLocationLng(),
                    request.getDestinationLat(),
                    request.getDestinationLng()
            );
        }

        // Create a new trip tracking
        TripTracking trip = TripTracking.builder()
                .tripId(request.getTripId())
                .userId(request.getUserId())
                .vehicleId(request.getVehicleId())
                .driverId(request.getDriverId())
                .startLocationLat(request.getStartLocationLat())
                .startLocationLng(request.getStartLocationLng())
                .startLocationGeohash(startLocationGeohash)
                .destinationLat(request.getDestinationLat())
                .destinationLng(request.getDestinationLng())
                .destinationGeohash(destinationGeohash)
                .currentLocationLat(request.getStartLocationLat())
                .currentLocationLng(request.getStartLocationLng())
                .currentLocationGeohash(startLocationGeohash)
                .status(TripTracking.TripStatus.WAITING.name())
                .distanceKm(distance)
                .startTime(ZonedDateTime.now())
                .isShared(false)
                .build();

        TripTracking savedTrip = tripTrackingRepository.save(trip);

        // Publish trip start event to Kafka
        kafkaTemplate.send(tripEventsTopic, savedTrip.getTripId().toString(), "TRIP_STARTED");
        log.info("Published trip start event for tripId: {}", savedTrip.getTripId());

        return trackingMapper.toTripTrackingResponse(savedTrip);
    }

    /**
     * Get trip tracking details by trip ID
     */
    public TripTrackingResponse getTripById(UUID tripId) {
        TripTracking trip = tripTrackingRepository.findByTripId(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with ID: " + tripId));

        // Update with latest vehicle location
        updateTripWithLatestLocation(trip);

        return trackingMapper.toTripTrackingResponse(trip);
    }

    /**
     * Get trip tracking details by share code
     */
    public TripTrackingResponse getSharedTrip(String shareCode) {
        TripTracking trip = tripTrackingRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Trip not found with share code: " + shareCode));

        // Update with latest vehicle location
        updateTripWithLatestLocation(trip);

        return trackingMapper.toTripTrackingResponse(trip);
    }

    /**
     * Get all active trips for a user
     */
    public List<TripTrackingResponse> getUserActiveTrips(UUID userId) {
        List<TripTracking> activeTrips = tripTrackingRepository.findByUserIdAndStatus(userId,
                TripTracking.TripStatus.IN_PROGRESS.name());

        activeTrips.addAll(tripTrackingRepository.findByUserIdAndStatus(userId,
                TripTracking.TripStatus.STARTED.name()));

        activeTrips.addAll(tripTrackingRepository.findByUserIdAndStatus(userId,
                TripTracking.TripStatus.WAITING.name()));

        // Update each trip with the latest vehicle location
        activeTrips.forEach(this::updateTripWithLatestLocation);

        return activeTrips.stream()
                .map(trackingMapper::toTripTrackingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all trips for a user
     */
    public List<TripTrackingResponse> getAllTripsByUserId(UUID userId) {
        List<TripTracking> trips = tripTrackingRepository.findByUserId(userId);

        // Update each trip with the latest vehicle location
        trips.forEach(this::updateTripWithLatestLocation);

        return trips.stream()
                .map(trackingMapper::toTripTrackingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all trips for a vehicle with a specific status
     */
    public List<TripTrackingResponse> getTripsByVehicleIdAndStatus(UUID vehicleId, String status) {
        validateTripStatus(status);
        List<TripTracking> trips = tripTrackingRepository.findByVehicleIdAndStatus(vehicleId, status);

        // Update each trip with the latest vehicle location
        trips.forEach(this::updateTripWithLatestLocation);

        return trips.stream()
                .map(trackingMapper::toTripTrackingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all trips for a driver with a specific status
     */
    public List<TripTrackingResponse> getTripsByDriverIdAndStatus(UUID driverId, String status) {
        validateTripStatus(status);
        List<TripTracking> trips = tripTrackingRepository.findByDriverIdAndStatus(driverId, status);

        // Update each trip with the latest vehicle location
        trips.forEach(this::updateTripWithLatestLocation);

        return trips.stream()
                .map(trackingMapper::toTripTrackingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all trips with a specific status
     */
    public List<TripTrackingResponse> getTripsByStatus(String status) {
        validateTripStatus(status);
        List<TripTracking> trips = tripTrackingRepository.findByStatus(status);

        // Update each trip with the latest vehicle location
        trips.forEach(this::updateTripWithLatestLocation);

        return trips.stream()
                .map(trackingMapper::toTripTrackingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update trip status
     */
    @Transactional
    public TripTrackingResponse updateTripStatus(UUID tripId, String status) {
        validateTripStatus(status);
        TripTracking trip = tripTrackingRepository.findByTripId(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with ID: " + tripId));

        trip.setStatus(status);

        // Special handling for specific statuses
        if (status.equals(TripTracking.TripStatus.STARTED.name())) {
            trip.setStartTime(ZonedDateTime.now());
        } else if (status.equals(TripTracking.TripStatus.COMPLETED.name())) {
            // Set current location to destination when completed
            trip.setCurrentLocationLat(trip.getDestinationLat());
            trip.setCurrentLocationLng(trip.getDestinationLng());
            trip.setCurrentLocationGeohash(trip.getDestinationGeohash());
            // Publish trip completion event to Kafka
            kafkaTemplate.send(tripEventsTopic, tripId.toString(), "TRIP_COMPLETED");
            log.info("Published trip completion event for tripId: {}", tripId);
        }

        // Save and notify
        updateAndNotifyTrip(trip);

        return trackingMapper.toTripTrackingResponse(trip);
    }

    /**
     * Share a trip and generate a share code
     */
    @Transactional
    public TripShareResponse shareTrip(TripShareRequest request) {
        TripTracking trip = tripTrackingRepository.findByTripId(request.getTripId())
                .orElseThrow(() -> new RuntimeException("Trip not found with ID: " + request.getTripId()));

        // Verify the user owns the trip
        if (!trip.getUserId().equals(request.getUserId())) {
            throw new RuntimeException("User is not authorized to share this trip");
        }

        // Generate share code if requested or not already shared
        if ((request.getGenerateShareCode() != null && request.getGenerateShareCode())
                || (trip.getShareCode() == null || trip.getShareCode().isEmpty())) {
            trip.setShareCode(generateShareCode());
        }

        trip.setShared(true);
        TripTracking updatedTrip = tripTrackingRepository.save(trip);

        // Publish trip share event to Kafka
        kafkaTemplate.send(tripEventsTopic, updatedTrip.getTripId().toString(), "TRIP_SHARED");
        log.info("Published trip share event for tripId: {}", updatedTrip.getTripId());

        return trackingMapper.toTripShareResponse(updatedTrip);
    }

    /**
     * Find vehicles near a given location
     */
    public List<LocationResponse> findVehiclesNear(VehicleProximityRequest request) {
        double radiusKm = request.getRadiusKm() != null ? request.getRadiusKm() : 2.0;
        int limit = request.getLimit() != null ? request.getLimit() : 10;

        // Get all geohashes within the radius
        Set<String> geohashes = geoHashUtil.getGeohashesWithinRadius(request.getLatitude(), request.getLongitude(), radiusKm);

        // Search for vehicles within these geohashes
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusMinutes(2); // Only get vehicles with updates in the last 2 minutes
        List<VehicleLocation> nearbyVehicles = vehicleLocationRepository.findByGeohashesAndRecent(
                new ArrayList<>(geohashes), cutoffTime);

        // Deduplicate by taking only the latest location for each vehicle
        Map<UUID, VehicleLocation> latestLocations = new HashMap<>();
        for (VehicleLocation location : nearbyVehicles) {
            latestLocations.putIfAbsent(location.getVehicleId(), location);

            VehicleLocation existingLocation = latestLocations.get(location.getVehicleId());
            if (location.getTimestamp().isAfter(existingLocation.getTimestamp())) {
                latestLocations.put(location.getVehicleId(), location);
            }
        }

        // Filter by distance and take the closest ones up to the limit
        return latestLocations.values().stream()
                .filter(loc -> geoHashUtil.calculateDistance(
                        request.getLatitude(), request.getLongitude(),
                        loc.getLatitude(), loc.getLongitude()) <= radiusKm)
                .sorted((loc1, loc2) -> {
                    double dist1 = geoHashUtil.calculateDistance(
                            request.getLatitude(), request.getLongitude(),
                            loc1.getLatitude(), loc1.getLongitude());
                    double dist2 = geoHashUtil.calculateDistance(
                            request.getLatitude(), request.getLongitude(),
                            loc2.getLatitude(), loc2.getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .limit(limit)
                .map(trackingMapper::toLocationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get the latest location for a vehicle
     */
    public LocationResponse getLatestVehicleLocation(UUID vehicleId) {
        VehicleLocation latestLocation = vehicleLocationRepository.findTopByVehicleIdOrderByTimestampDesc(vehicleId);
        if (latestLocation == null) {
            throw new RuntimeException("No location found for vehicleId: " + vehicleId);
        }
        return trackingMapper.toLocationResponse(latestLocation);
    }

    /**
     * Get location history for a vehicle within a time range
     */
    public List<LocationResponse> getVehicleLocationHistory(UUID vehicleId, ZonedDateTime startTime, ZonedDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        List<VehicleLocation> locations = vehicleLocationRepository.findByVehicleIdAndTimestampBetweenOrderByTimestampAsc(
                vehicleId, startTime, endTime);
        return locations.stream()
                .map(trackingMapper::toLocationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Clean up old vehicle locations
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    public void cleanupOldLocations() {
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusDays(30);
        vehicleLocationRepository.deleteByTimestampBefore(cutoffTime);
        log.info("Cleaned up vehicle locations older than {}", cutoffTime);
    }

    /**
     * Validate trip status against TripStatus enum
     */
    private void validateTripStatus(String status) {
        if (status != null) {
            try {
                TripTracking.TripStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid trip status: " + status + ". Must be one of: " +
                        Arrays.stream(TripTracking.TripStatus.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", ")));
            }
        }
    }
}