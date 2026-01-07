package com.p2p.booking.service;

import com.p2p.booking.entity.Provider;
import com.p2p.booking.entity.Route;
import com.p2p.booking.entity.Trip;
import com.p2p.booking.entity.Vehicle;
import com.p2p.booking.repository.ProviderRepository;
import com.p2p.booking.repository.RouteRepository;
import com.p2p.booking.repository.TripRepository;
import com.p2p.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GeoHashIndexingService {
    
    private final GeoHashService geoHashService;
    private final ProviderRepository providerRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    
    /**
     * Update GeoHash for a provider's base location
     */
    public void updateProviderGeoHash(Provider provider) {
        if (provider.getBaseLatitude() != null && provider.getBaseLongitude() != null) {
            String geoHash = geoHashService.generateProviderGeoHash(
                provider.getBaseLatitude(), 
                provider.getBaseLongitude()
            );
            provider.setGeoHash(geoHash);
            providerRepository.save(provider);
            log.debug("Updated GeoHash for provider {}: {}", provider.getId(), geoHash);
        }
    }
    
    /**
     * Update GeoHash for a vehicle's current location
     */
    public void updateVehicleGeoHash(Vehicle vehicle) {
        if (vehicle.getCurrentLatitude() != null && vehicle.getCurrentLongitude() != null) {
            String geoHash = geoHashService.generateProviderGeoHash(
                vehicle.getCurrentLatitude(), 
                vehicle.getCurrentLongitude()
            );
            vehicle.setCurrentGeoHash(geoHash);
            vehicle.setLastLocationUpdate(java.time.LocalDateTime.now());
            vehicleRepository.save(vehicle);
            log.debug("Updated GeoHash for vehicle {}: {}", vehicle.getId(), geoHash);
        }
    }
    
    /**
     * Update GeoHash indexes for a route including waypoints
     */
    public void updateRouteGeoHash(Route route) {
        // Update start and end GeoHashes
        if (route.getStartLatitude() != null && route.getStartLongitude() != null) {
            route.setStartGeoHash(geoHashService.generateRouteGeoHash(
                route.getStartLatitude(), route.getStartLongitude()
            ));
        }
        
        if (route.getEndLatitude() != null && route.getEndLongitude() != null) {
            route.setEndGeoHash(geoHashService.generateRouteGeoHash(
                route.getEndLatitude(), route.getEndLongitude()
            ));
        }
        
        // Generate waypoint GeoHashes for intermediate points
        if (route.getStartLatitude() != null && route.getStartLongitude() != null &&
            route.getEndLatitude() != null && route.getEndLongitude() != null) {
            
            List<String> waypoints = geoHashService.generateRouteWaypoints(
                route.getStartLatitude(), route.getStartLongitude(),
                route.getEndLatitude(), route.getEndLongitude(),
                5 // Generate 5 waypoints for route indexing
            );
            
            route.setWaypointGeoHashList(waypoints);
        }
        
        routeRepository.save(route);
        log.debug("Updated GeoHash for route {}: start={}, end={}, waypoints={}", 
                 route.getId(), route.getStartGeoHash(), route.getEndGeoHash(), 
                 route.getWaypointGeoHashList().size());
    }
    
    /**
     * Update GeoHash for trip pickup and dropoff locations
     */
    public void updateTripGeoHash(Trip trip) {
        // Update pickup GeoHash
        if (trip.getPickupLatitude() != null && trip.getPickupLongitude() != null) {
            trip.setPickupGeoHash(geoHashService.generateGeoHash(
                trip.getPickupLatitude(), trip.getPickupLongitude()
            ));
        }
        
        // Update dropoff GeoHash
        if (trip.getDropoffLatitude() != null && trip.getDropoffLongitude() != null) {
            trip.setDropoffGeoHash(geoHashService.generateGeoHash(
                trip.getDropoffLatitude(), trip.getDropoffLongitude()
            ));
        }
        
        tripRepository.save(trip);
        log.debug("Updated GeoHash for trip {}: pickup={}, dropoff={}", 
                 trip.getId(), trip.getPickupGeoHash(), trip.getDropoffGeoHash());
    }
    
    /**
     * Bulk update GeoHash indexes for all entities
     */
    public void rebuildAllGeoHashIndexes() {
        log.info("Starting GeoHash index rebuild for all entities");
        
        // Update providers
        List<Provider> providers = providerRepository.findAll();
        for (Provider provider : providers) {
            updateProviderGeoHash(provider);
        }
        log.info("Updated GeoHash for {} providers", providers.size());
        
        // Update vehicles
        List<Vehicle> vehicles = vehicleRepository.findAll();
        for (Vehicle vehicle : vehicles) {
            updateVehicleGeoHash(vehicle);
        }
        log.info("Updated GeoHash for {} vehicles", vehicles.size());
        
        // Update routes
        List<Route> routes = routeRepository.findAll();
        for (Route route : routes) {
            updateRouteGeoHash(route);
        }
        log.info("Updated GeoHash for {} routes", routes.size());
        
        // Update trips
        List<Trip> trips = tripRepository.findAll();
        for (Trip trip : trips) {
            updateTripGeoHash(trip);
        }
        log.info("Updated GeoHash for {} trips", trips.size());
        
        log.info("Completed GeoHash index rebuild");
    }
    
    /**
     * Update vehicle location with coordinates
     */
    public void updateVehicleLocation(Long vehicleId, BigDecimal latitude, BigDecimal longitude) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        
        vehicle.setCurrentLatitude(latitude);
        vehicle.setCurrentLongitude(longitude);
        updateVehicleGeoHash(vehicle);
        
        log.info("Updated location for vehicle {}: {}, {}", vehicleId, latitude, longitude);
    }
    
    /**
     * Find providers within radius using GeoHash
     */
    public List<Provider> findProvidersInRadius(BigDecimal latitude, BigDecimal longitude, 
                                               double radiusKm) {
        String centerGeoHash = geoHashService.generateProviderGeoHash(latitude, longitude);
        List<String> searchGeoHashes = geoHashService.getNeighboringGeoHashes(centerGeoHash);
        
        return providerRepository.findByGeoHashIn(searchGeoHashes).stream()
            .filter(provider -> {
                if (provider.getBaseLatitude() == null || provider.getBaseLongitude() == null) {
                    return false;
                }
                
                double distance = geoHashService.calculateDistance(centerGeoHash, provider.getGeoHash());
                return distance <= radiusKm;
            })
            .toList();
    }
}