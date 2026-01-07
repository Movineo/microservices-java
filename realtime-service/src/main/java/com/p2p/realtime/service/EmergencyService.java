package com.p2p.realtime.service;

import com.p2p.realtime.dto.EmergencyRequest;
import com.p2p.realtime.dto.EmergencyResponse;
import com.p2p.realtime.dto.WebSocketMessage;
import com.p2p.realtime.model.EmergencyCase;
import com.p2p.realtime.repository.EmergencyCaseRepository;
import com.p2p.realtime.util.EmergencyMapper;
import com.p2p.realtime.util.GeoHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private static final int GEOHASH_PREFIX_LENGTH = 4;
    private static final double NEARBY_NOTIFICATION_RADIUS_KM = 5.0;

    private final EmergencyCaseRepository emergencyCaseRepository;
    private final GeoHashUtil geoHashUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final GeoSearchService geoSearchService;

    /**
     * Report a new emergency case with validation and WebSocket notifications
     */
    @Transactional
    public EmergencyResponse reportEmergency(EmergencyRequest request) {
        validateRequest(request);

        // Generate geohash for the location
        String geohash = geoHashUtil.encode(request.getLatitude(), request.getLongitude());

        // Create and save the emergency case
        EmergencyCase emergency = EmergencyCase.builder()
                .userId(request.getUserId())
                .tripId(request.getTripId())
                .emergencyType(request.getEmergencyType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geohash(geohash)
                .message(request.getMessage())
                .build(); // resolved, sharedWithContacts, sharedWithAuthorities default to false

        EmergencyCase savedEmergency = emergencyCaseRepository.save(emergency);
        EmergencyResponse response = EmergencyMapper.buildEmergencyResponse(savedEmergency);

        // Send WebSocket notifications
        WebSocketMessage<EmergencyResponse> wsMessage = WebSocketMessage.create(
                WebSocketMessage.MessageType.EMERGENCY.name(), response
        );
        messagingTemplate.convertAndSend("/topic/emergency", wsMessage);
        if (StringUtils.hasText(geohash) && geohash.length() >= GEOHASH_PREFIX_LENGTH) {
            messagingTemplate.convertAndSend(
                    "/topic/emergency/area/" + geohash.substring(0, GEOHASH_PREFIX_LENGTH), wsMessage
            );
        }

        // Notify users in nearby areas
        List<EmergencyResponse> nearbyEmergencies = geoSearchService.findNearbyEmergencies(
                request.getLatitude(), request.getLongitude(), NEARBY_NOTIFICATION_RADIUS_KM
        );
        nearbyEmergencies.stream()
                .filter(e -> !e.getUserId().equals(request.getUserId())) // Exclude the reporting user
                .forEach(nearby -> messagingTemplate.convertAndSendToUser(
                        nearby.getUserId().toString(),
                        "/queue/emergency/nearby",
                        WebSocketMessage.create(WebSocketMessage.MessageType.NEARBY_EMERGENCY.name(), response)
                ));

        // TODO: Implement notification to authorities and contacts if requested
        return response;
    }

    /**
     * Get all active emergency cases with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "activeEmergencies", unless = "#result.isEmpty()")
    public List<EmergencyResponse> getAllActiveEmergencies() {
        return emergencyCaseRepository.findByResolvedFalse().stream()
                .map(EmergencyMapper::buildEmergencyResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active emergency cases for a user with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userEmergencies", key = "#userId", unless = "#result.isEmpty()")
    public List<EmergencyResponse> getUserEmergencies(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return emergencyCaseRepository.findByUserIdAndResolvedFalse(userId).stream()
                .map(EmergencyMapper::buildEmergencyResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active emergency cases for a trip with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tripEmergencies", key = "#tripId", unless = "#result.isEmpty()")
    public List<EmergencyResponse> getTripEmergencies(UUID tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("Trip ID cannot be null");
        }
        return emergencyCaseRepository.findByTripIdAndResolvedFalse(tripId).stream()
                .map(EmergencyMapper::buildEmergencyResponse)
                .collect(Collectors.toList());
    }

    /**
     * Resolve an emergency case with WebSocket notification
     */
    @Transactional
    public EmergencyResponse resolveEmergency(UUID emergencyId) {
        if (emergencyId == null) {
            throw new IllegalArgumentException("Emergency ID cannot be null");
        }
        EmergencyCase emergency = emergencyCaseRepository.findById(emergencyId)
                .orElseThrow(() -> new RuntimeException("Emergency case not found with ID: " + emergencyId));

        emergency.setResolved(true);
        emergency.setResolvedAt(ZonedDateTime.now());
        EmergencyCase resolvedEmergency = emergencyCaseRepository.save(emergency);

        // Send WebSocket notifications
        EmergencyResponse response = EmergencyMapper.buildEmergencyResponse(resolvedEmergency);
        WebSocketMessage<EmergencyResponse> wsMessage = WebSocketMessage.create(
                WebSocketMessage.MessageType.EMERGENCY.name(), response
        );
        messagingTemplate.convertAndSend("/topic/emergency", wsMessage);
        messagingTemplate.convertAndSendToUser(
                emergency.getUserId().toString(),
                "/queue/emergency",
                wsMessage
        );

        return response;
    }

    /**
     * Validate EmergencyRequest
     */
    private void validateRequest(EmergencyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Emergency request cannot be null");
        }
        // Rely on Jakarta Bean Validation for userId, emergencyType, latitude, longitude
    }
}