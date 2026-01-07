package com.p2p.realtime.service;

import com.p2p.realtime.dto.EmergencyResponse;
import com.p2p.realtime.repository.EmergencyCaseRepository;
import com.p2p.realtime.util.EmergencyMapper;
import com.p2p.realtime.util.GeoHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeoSearchService {

    private final EmergencyCaseRepository emergencyCaseRepository;
    private final GeoHashUtil geoHashUtil;

    /**
     * Find active emergency cases within a radius
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "nearbyEmergencies", key = "{#latitude, #longitude, #radiusKm}", unless = "#result.isEmpty()")
    public List<EmergencyResponse> findNearbyEmergencies(double latitude, double longitude, double radiusKm) {
        if (radiusKm < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        Set<String> nearbyGeohashes = geoHashUtil.getGeohashesWithinRadius(latitude, longitude, radiusKm);
        return emergencyCaseRepository.findByGeohashInAndResolvedFalse(nearbyGeohashes).stream()
                .map(EmergencyMapper::buildEmergencyResponse)
                .collect(Collectors.toList());
    }
}