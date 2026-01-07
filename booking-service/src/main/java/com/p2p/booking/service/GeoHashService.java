package com.p2p.booking.service;

import ch.hsr.geohash.GeoHash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeoHashService {
    
    private static final int DEFAULT_PRECISION = 7; // ~153m precision
    private static final int ROUTE_PRECISION = 6;   // ~610m precision for route waypoints
    private static final int PROVIDER_PRECISION = 7; // ~153m precision for provider locations
    
    /**
     * Generate GeoHash for a given latitude and longitude
     */
    public String generateGeoHash(BigDecimal latitude, BigDecimal longitude, int precision) {
        if (latitude == null || longitude == null) {
            return null;
        }
        
        try {
            GeoHash geoHash = GeoHash.withCharacterPrecision(
                latitude.doubleValue(), 
                longitude.doubleValue(), 
                precision
            );
            return geoHash.toBase32();
        } catch (Exception e) {
            log.error("Error generating GeoHash for lat: {}, lon: {}", latitude, longitude, e);
            return null;
        }
    }
    
    /**
     * Generate GeoHash with default precision
     */
    public String generateGeoHash(BigDecimal latitude, BigDecimal longitude) {
        return generateGeoHash(latitude, longitude, DEFAULT_PRECISION);
    }
    
    /**
     * Generate GeoHash for route waypoints (lower precision for broader matching)
     */
    public String generateRouteGeoHash(BigDecimal latitude, BigDecimal longitude) {
        return generateGeoHash(latitude, longitude, ROUTE_PRECISION);
    }
    
    /**
     * Generate GeoHash for provider locations (higher precision for accurate matching)
     */
    public String generateProviderGeoHash(BigDecimal latitude, BigDecimal longitude) {
        return generateGeoHash(latitude, longitude, PROVIDER_PRECISION);
    }
    
    /**
     * Get neighboring GeoHashes for broader search area
     */
    public List<String> getNeighboringGeoHashes(String geoHashString) {
        if (geoHashString == null || geoHashString.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            GeoHash geoHash = GeoHash.fromGeohashString(geoHashString);
            GeoHash[] neighbors = geoHash.getAdjacent();
            
            List<String> neighborHashes = new ArrayList<>();
            neighborHashes.add(geoHashString); // Include the center
            
            for (GeoHash neighbor : neighbors) {
                neighborHashes.add(neighbor.toBase32());
            }
            
            return neighborHashes;
        } catch (Exception e) {
            log.error("Error getting neighboring GeoHashes for: {}", geoHashString, e);
            return List.of(geoHashString);
        }
    }
    
    /**
     * Get GeoHash prefixes for radius-based search
     */
    public List<String> getGeoHashPrefixes(String geoHashString, int prefixLength) {
        if (geoHashString == null || geoHashString.isEmpty() || prefixLength <= 0) {
            return new ArrayList<>();
        }
        
        List<String> prefixes = new ArrayList<>();
        
        // Get the main prefix
        String mainPrefix = geoHashString.length() >= prefixLength ? 
            geoHashString.substring(0, prefixLength) : geoHashString;
        prefixes.add(mainPrefix);
        
        // Get neighboring prefixes for broader coverage
        try {
            GeoHash geoHash = GeoHash.fromGeohashString(geoHashString);
            GeoHash[] neighbors = geoHash.getAdjacent();
            
            for (GeoHash neighbor : neighbors) {
                String neighborStr = neighbor.toBase32();
                String neighborPrefix = neighborStr.length() >= prefixLength ? 
                    neighborStr.substring(0, prefixLength) : neighborStr;
                
                if (!prefixes.contains(neighborPrefix)) {
                    prefixes.add(neighborPrefix);
                }
            }
        } catch (Exception e) {
            log.error("Error getting GeoHash prefixes for: {}", geoHashString, e);
        }
        
        return prefixes;
    }
    
    /**
     * Calculate distance between two GeoHashes in kilometers
     */
    public double calculateDistance(String geoHash1, String geoHash2) {
        if (geoHash1 == null || geoHash2 == null) {
            return Double.MAX_VALUE;
        }
        
        try {
            GeoHash gh1 = GeoHash.fromGeohashString(geoHash1);
            GeoHash gh2 = GeoHash.fromGeohashString(geoHash2);
            
            return calculateHaversineDistance(
                gh1.getBoundingBoxCenter().getLatitude(), gh1.getBoundingBoxCenter().getLongitude(),
                gh2.getBoundingBoxCenter().getLatitude(), gh2.getBoundingBoxCenter().getLongitude()
            );
        } catch (Exception e) {
            log.error("Error calculating distance between GeoHashes: {} and {}", geoHash1, geoHash2, e);
            return Double.MAX_VALUE;
        }
    }
    
    /**
     * Check if two GeoHashes are within a specified radius (in kilometers)
     */
    public boolean isWithinRadius(String geoHash1, String geoHash2, double radiusKm) {
        double distance = calculateDistance(geoHash1, geoHash2);
        return distance <= radiusKm;
    }
    
    /**
     * Generate route waypoint GeoHashes between start and end points
     */
    public List<String> generateRouteWaypoints(BigDecimal startLat, BigDecimal startLon, 
                                              BigDecimal endLat, BigDecimal endLon, 
                                              int numberOfWaypoints) {
        List<String> waypoints = new ArrayList<>();
        
        if (startLat == null || startLon == null || endLat == null || endLon == null) {
            return waypoints;
        }
        
        // Always add start point
        waypoints.add(generateRouteGeoHash(startLat, startLon));
        
        // Generate intermediate waypoints
        for (int i = 1; i < numberOfWaypoints; i++) {
            double ratio = (double) i / numberOfWaypoints;
            
            BigDecimal waypointLat = startLat.add(
                endLat.subtract(startLat).multiply(BigDecimal.valueOf(ratio))
            );
            BigDecimal waypointLon = startLon.add(
                endLon.subtract(startLon).multiply(BigDecimal.valueOf(ratio))
            );
            
            String waypointHash = generateRouteGeoHash(waypointLat, waypointLon);
            if (waypointHash != null && !waypoints.contains(waypointHash)) {
                waypoints.add(waypointHash);
            }
        }
        
        // Always add end point
        String endHash = generateRouteGeoHash(endLat, endLon);
        if (endHash != null && !waypoints.contains(endHash)) {
            waypoints.add(endHash);
        }
        
        return waypoints;
    }
    
    /**
     * Haversine distance calculation
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadiusKm * c;
    }
    
    /**
     * Get optimal GeoHash precision based on distance
     */
    public int getOptimalPrecision(double distanceKm) {
        if (distanceKm <= 1) return 8;      // ~19m precision
        if (distanceKm <= 5) return 7;      // ~153m precision
        if (distanceKm <= 25) return 6;     // ~610m precision
        if (distanceKm <= 125) return 5;    // ~2.4km precision
        return 4; // ~20km precision for very long distances
    }
}