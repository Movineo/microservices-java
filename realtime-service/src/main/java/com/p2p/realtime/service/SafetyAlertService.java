package com.p2p.realtime.service;

import com.p2p.realtime.dto.SafetyAlertRequest;
import com.p2p.realtime.dto.SafetyAlertResponse;
import com.p2p.realtime.dto.WebSocketMessage;
import com.p2p.realtime.model.SafetyAlert;
import com.p2p.realtime.repository.SafetyAlertRepository;
import com.p2p.realtime.util.EmergencyMapper;
import com.p2p.realtime.util.GeoHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyAlertService {

    private final SafetyAlertRepository safetyAlertRepository;
    private final GeoHashUtil geoHashUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int CACHE_TTL_MINUTES = 30;
    private static final String ALERTS_CACHE_PREFIX = "alerts:geohash:";

    /**
     * Create a new safety alert
     */
    @Transactional
    public SafetyAlertResponse createAlert(SafetyAlertRequest request) {
        // Create new alert
        SafetyAlert alert = SafetyAlert.builder()
                .alertType(request.getAlertType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geohash(geoHashUtil.encode(request.getLatitude(), request.getLongitude()))
                .message(request.getMessage())
                .severity(request.getSeverity())
                .active(true)
                .radiusKm(request.getRadiusKm())
                .reportedBy(request.getReportedBy())
                .verified(false)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        SafetyAlert savedAlert = safetyAlertRepository.save(alert);

        // Cache the alert by geohash prefix (first 4 characters)
        String geohash = savedAlert.getGeohash();
        if (geohash.length() >= 4) {
            String geohashPrefix = geohash.substring(0, 4);
            String cacheKey = ALERTS_CACHE_PREFIX + geohashPrefix;
            redisTemplate.opsForList().leftPush(cacheKey, savedAlert);
            redisTemplate.expire(cacheKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            // Notify subscribers in the area
            SafetyAlertResponse response = EmergencyMapper.buildSafetyAlertResponse(savedAlert);
            messagingTemplate.convertAndSend("/topic/alerts/area/" + geohashPrefix,
                    WebSocketMessage.create(WebSocketMessage.MessageType.SAFETY_ALERT.name(), response));
            return response;
        }

        return EmergencyMapper.buildSafetyAlertResponse(savedAlert);
    }

    /**
     * Get all active alerts
     */
    public List<SafetyAlertResponse> getAllActiveAlerts() {
        return safetyAlertRepository.findByActiveTrue().stream()
                .map(EmergencyMapper::buildSafetyAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by geohash prefix
     */
    public List<SafetyAlertResponse> getAlertsByGeohashPrefix(String geohashPrefix) {
        String cacheKey = ALERTS_CACHE_PREFIX + geohashPrefix;

        // Try to get from cache first
        List<Object> cachedAlerts = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cachedAlerts != null && !cachedAlerts.isEmpty()) {
            return cachedAlerts.stream()
                    .map(obj -> EmergencyMapper.buildSafetyAlertResponse((SafetyAlert) obj))
                    .collect(Collectors.toList());
        }

        // Fall back to the database
        List<SafetyAlert> alerts = safetyAlertRepository.findActiveAlertsByGeohashPrefixOrRadius(
                geohashPrefix, 5.0); // Get alerts with radius > 5 km as well

        // Update cache
        if (!alerts.isEmpty()) {
            for (SafetyAlert alert : alerts) {
                redisTemplate.opsForList().leftPush(cacheKey, alert);
            }
            redisTemplate.expire(cacheKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return alerts.stream()
                .map(EmergencyMapper::buildSafetyAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts near a location
     */
    public List<SafetyAlertResponse> getAlertsNearLocation(double latitude, double longitude, double radiusKm) {
        // Get all geohashes within the radius
        Set<String> geohashes = geoHashUtil.getGeohashesWithinRadius(latitude, longitude, radiusKm);

        // Search for alerts within these geohashes
        List<SafetyAlert> alerts = safetyAlertRepository.findActiveAlertsByGeohashes(
                new ArrayList<>(geohashes));

        // Filter by actual distance and add large radius alerts
        return alerts.stream()
                .filter(alert -> {
                    double distance = geoHashUtil.calculateDistance(
                            latitude, longitude,
                            alert.getLatitude(), alert.getLongitude());
                    return distance <= radiusKm ||
                            (alert.getRadiusKm() != null && distance <= alert.getRadiusKm());
                })
                .map(EmergencyMapper::buildSafetyAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an alert's status (active/inactive)
     */
    @Transactional
    public SafetyAlertResponse updateAlertStatus(UUID alertId, boolean active) {
        SafetyAlert alert = safetyAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with ID: " + alertId));

        alert.setActive(active);
        if (!active && alert.getEndTime() == null) {
            alert.setEndTime(ZonedDateTime.now());
        }

        SafetyAlert updatedAlert = safetyAlertRepository.save(alert);

        // Notify subscribers in the area about the update
        String geohash = updatedAlert.getGeohash();
        if (geohash.length() >= 4) {
            String geohashPrefix = geohash.substring(0, 4);
            SafetyAlertResponse response = EmergencyMapper.buildSafetyAlertResponse(updatedAlert);
            messagingTemplate.convertAndSend("/topic/alerts/area/" + geohashPrefix,
                    WebSocketMessage.create(WebSocketMessage.MessageType.SAFETY_ALERT.name(), response));
            return response;
        }

        return EmergencyMapper.buildSafetyAlertResponse(updatedAlert);
    }

    /**
     * Verify an alert
     */
    @Transactional
    public SafetyAlertResponse verifyAlert(UUID alertId) {
        SafetyAlert alert = safetyAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with ID: " + alertId));

        alert.setVerified(true);
        SafetyAlert updatedAlert = safetyAlertRepository.save(alert);

        return EmergencyMapper.buildSafetyAlertResponse(updatedAlert);
    }

    /**
     * Get active alerts reported by a specific user
     */
    public List<SafetyAlertResponse> getAlertsByReportedBy(UUID reportedBy) {
        return safetyAlertRepository.findByReportedByAndActiveTrue(reportedBy).stream()
                .map(EmergencyMapper::buildSafetyAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active alerts by alert type
     */
    public List<SafetyAlertResponse> getAlertsByAlertType(String alertType) {
        return safetyAlertRepository.findByAlertTypeAndActiveTrue(alertType).stream()
                .map(EmergencyMapper::buildSafetyAlertResponse)
                .collect(Collectors.toList());
    }
}