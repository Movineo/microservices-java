package com.p2p.realtime.util;

import com.p2p.realtime.dto.LocationResponse;
import com.p2p.realtime.dto.TripShareResponse;
import com.p2p.realtime.dto.TripTrackingResponse;
import com.p2p.realtime.model.TripTracking;
import com.p2p.realtime.model.VehicleLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class TrackingMapper {

    private final GeoHashUtil geoHashUtil;

    /**
     * Convert VehicleLocation entity to LocationResponse DTO
     */
    public LocationResponse toLocationResponse(VehicleLocation location) {
        return LocationResponse.builder()
                .vehicleId(location.getVehicleId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .geohash(location.getGeohash())
                .speed(location.getSpeed())
                .bearing(location.getBearing())
                .timestamp(location.getTimestamp())
                .build();
    }

    /**
     * Convert TripTracking entity to TripTrackingResponse DTO
     */
    public TripTrackingResponse toTripTrackingResponse(TripTracking trip) {
        // Calculate completed and remaining distance
        Double completedDistance = null;
        Double remainingDistance = null;
        Integer etaMinutes = null;

        if (trip.getCurrentLocationLat() != null && trip.getCurrentLocationLng() != null) {
            completedDistance = geoHashUtil.calculateDistance(
                    trip.getStartLocationLat(),
                    trip.getStartLocationLng(),
                    trip.getCurrentLocationLat(),
                    trip.getCurrentLocationLng()
            );

            remainingDistance = geoHashUtil.calculateDistance(
                    trip.getCurrentLocationLat(),
                    trip.getCurrentLocationLng(),
                    trip.getDestinationLat(),
                    trip.getDestinationLng()
            );

            if (trip.getEstimatedArrivalTime() != null) {
                long minutesUntilArrival = ZonedDateTime.now().until(trip.getEstimatedArrivalTime(), java.time.temporal.ChronoUnit.MINUTES);
                etaMinutes = (int) minutesUntilArrival;
            }
        }

        return TripTrackingResponse.builder()
                .tripId(trip.getTripId())
                .userId(trip.getUserId())
                .vehicleId(trip.getVehicleId())
                .driverId(trip.getDriverId())
                .startLocationLat(trip.getStartLocationLat())
                .startLocationLng(trip.getStartLocationLng())
                .destinationLat(trip.getDestinationLat())
                .destinationLng(trip.getDestinationLng())
                .currentLocationLat(trip.getCurrentLocationLat())
                .currentLocationLng(trip.getCurrentLocationLng())
                .startTime(trip.getStartTime())
                .estimatedArrivalTime(trip.getEstimatedArrivalTime())
                .status(trip.getStatus())
                .distanceKm(trip.getDistanceKm())
                .completedDistanceKm(completedDistance)
                .remainingDistanceKm(remainingDistance)
                .estimatedMinutesRemaining(etaMinutes)
                .shareCode(trip.getShareCode())
                .isShared(trip.isShared())
                .build();
    }

    /**
     * Convert TripTracking entity to TripShareResponse DTO
     */
    public TripShareResponse toTripShareResponse(TripTracking trip) {
        return TripShareResponse.builder()
                .tripId(trip.getTripId())
                .isShared(trip.isShared())
                .shareCode(trip.getShareCode())
                .shareUrl("/tracking/shared/" + trip.getShareCode())
                .build();
    }
}