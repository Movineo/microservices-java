package com.p2p.realtime.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.queries.GeoHashCircleQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class GeoHashUtil {

    private static final double EARTH_RADIUS_KM = 6371.0; // Earth's radius in kilometers
    private static final int NEIGHBOR_COUNT = 9; // Center + 8 neighbors

    @Value("${realtime.geohash.default-precision:7}")
    private int defaultPrecision;

    /**
     * Encode a latitude/longitude pair into a geohash string with default precision
     */
    public String encode(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        return encode(latitude, longitude, defaultPrecision);
    }

    /**
     * Encode a latitude/longitude pair into a geohash string with specified precision
     */
    public String encode(double latitude, double longitude, int precision) {
        validateCoordinates(latitude, longitude);
        if (precision < 1 || precision > 12) { // GeoHash max precision is 12
            throw new IllegalArgumentException("Precision must be between 1 and 12");
        }
        return GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, precision);
    }

    /**
     * Decode a geohash string into a latitude/longitude pair
     */
    public WGS84Point decode(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }
        try {
            GeoHash hash = GeoHash.fromGeohashString(geohash);
            WGS84Point point = hash.getOriginatingPoint();
            if (point == null) {
                throw new IllegalStateException("GeoHash returned null point for: " + geohash);
            }
            return point;
        } catch (IllegalArgumentException e) {
            log.error("Invalid geohash: {}", geohash, e);
            throw e;
        }
    }

    /**
     * Get neighboring geohashes for the given latitude/longitude within the specified radius
     */
    public Set<String> getGeohashesWithinRadius(double latitude, double longitude, double radiusInKm) {
        validateCoordinates(latitude, longitude);
        if (radiusInKm < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        WGS84Point center = new WGS84Point(latitude, longitude);
        double radiusInMeters = radiusInKm * 1000; // Convert to meters once
        GeoHashCircleQuery query = new GeoHashCircleQuery(center, radiusInMeters);

        // Initialize HashSet with estimated size to avoid resizing
        Set<String> geohashes = new HashSet<>(query.getSearchHashes().size());
        for (GeoHash hash : query.getSearchHashes()) {
            geohashes.add(hash.toBase32());
        }
        return geohashes;
    }

    /**
     * Get neighboring geohash cells (9 cells including the center)
     */
    public Set<String> getNeighboringGeohashes(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }
        GeoHash hash = GeoHash.fromGeohashString(geohash);
        Set<String> neighbors = new HashSet<>(NEIGHBOR_COUNT); // Pre-size for 9 elements

        // Add center and neighbors efficiently
        neighbors.add(geohash);
        GeoHash north = hash.getNorthernNeighbour();
        GeoHash south = hash.getSouthernNeighbour();
        neighbors.add(north.toBase32());
        neighbors.add(south.toBase32());
        neighbors.add(hash.getEasternNeighbour().toBase32());
        neighbors.add(hash.getWesternNeighbour().toBase32());
        neighbors.add(north.getEasternNeighbour().toBase32());
        neighbors.add(north.getWesternNeighbour().toBase32());
        neighbors.add(south.getEasternNeighbour().toBase32());
        neighbors.add(south.getWesternNeighbour().toBase32());

        return neighbors;
    }

    /**
     * Calculate the distance between two points using the Haversine formula
     * @return distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double sinLatHalf = Math.sin(latDistance / 2);
        double sinLonHalf = Math.sin(lonDistance / 2);

        double a = sinLatHalf * sinLatHalf +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinLonHalf * sinLonHalf;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate estimated arrival time based on distance and speed
     * @param distanceInKm Distance in kilometers
     * @param speedKmh Speed in km/h
     * @return Estimated time in minutes
     */
    public int calculateETA(double distanceInKm, double speedKmh) {
        if (distanceInKm < 0 || speedKmh <= 0) {
            return 0; // Avoid invalid calculations
        }
        return (int) Math.round((distanceInKm / speedKmh) * 60); // Time in minutes
    }

    /**
     * Validate latitude and longitude values
     */
    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates: latitude must be [-90, 90], longitude must be [-180, 180]");
        }
    }
}