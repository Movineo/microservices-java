package com.p2p.booking.service;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.dto.BookingResponseDto;
import com.p2p.booking.entity.*;
import com.p2p.booking.events.BookingProcessedEvent;
import com.p2p.booking.events.TripStartedEvent;
import com.p2p.booking.events.TripCompletedEvent;
import com.p2p.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EnhancedBookingService {
    
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final ProviderRepository providerRepository;
    private final GeoHashService geoHashService;
    private final GeoHashIndexingService geoHashIndexingService;
    private final RouteOptimizationService routeOptimizationService;
    private final FareCalculationService fareCalculationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final double SEARCH_RADIUS_KM = 10.0; // 10km search radius
    
    /**
     * Process booking with GeoHash-optimized provider and vehicle matching
     */
    public BookingResponseDto processBookingWithGeoHash(String userId, String phoneNumber, 
                                                       BookingRequestDto request) {
        log.info("Processing GeoHash-optimized booking for user: {}", userId);
        
        try {
            // Generate GeoHashes for pickup and dropoff locations
            String pickupGeoHash = geoHashService.generateGeoHash(
                request.getStartLatitude(), request.getStartLongitude()
            );
            String dropoffGeoHash = geoHashService.generateGeoHash(
                request.getEndLatitude(), request.getEndLongitude()
            );
            
            // Find optimal routes using GeoHash-based search
            List<Route> optimalRoutes = findOptimalRoutesWithGeoHash(request, pickupGeoHash, dropoffGeoHash);
            if (optimalRoutes.isEmpty()) {
                return createFailureResponse("No routes available for the requested journey");
            }
            
            Route selectedRoute = optimalRoutes.get(0);
            
            // Find available vehicle using GeoHash proximity
            Optional<Vehicle> availableVehicle = findNearestAvailableVehicle(
                pickupGeoHash, selectedRoute, request.getPassengerCount()
            );
            
            if (availableVehicle.isEmpty()) {
                return createFailureResponse("No available vehicles near your location");
            }
            
            Vehicle vehicle = availableVehicle.get();
            
            // Calculate fare with dynamic pricing
            BigDecimal totalFare = fareCalculationService.calculateTotalFare(
                selectedRoute, 
                request.getPassengerCount(), 
                request.getPreferredDateTime(), 
                request.getSpecialRequests()
            );
            
            // Create trip with GeoHash indexing
            String bookingId = generateBookingId();
            Trip trip = createTripWithGeoHash(bookingId, userId, phoneNumber, selectedRoute, 
                                            vehicle, totalFare, request, pickupGeoHash, dropoffGeoHash);
            
            Trip savedTrip = tripRepository.save(trip);
            
            // Update vehicle status and location
            vehicle.setStatus(Vehicle.VehicleStatus.IN_TRANSIT);
            vehicleRepository.save(vehicle);
            
            // Index trip GeoHash
            geoHashIndexingService.updateTripGeoHash(savedTrip);
            
            // Publish booking processed event
            BookingProcessedEvent event = createBookingProcessedEvent(savedTrip, "CONFIRMED");
            kafkaTemplate.send("booking-events", event);
            
            log.info("GeoHash-optimized booking processed successfully: {}", bookingId);
            return createSuccessResponse(savedTrip);
            
        } catch (Exception e) {
            log.error("Error processing GeoHash booking: {}", e.getMessage(), e);
            return createFailureResponse("Failed to process booking: " + e.getMessage());
        }
    }
    
    /**
     * Find available vehicles near pickup location using GeoHash
     */
    public List<Vehicle> getAvailableVehiclesNearLocation(BigDecimal latitude, BigDecimal longitude, 
                                                         Integer minCapacity, String providerType) {
        String locationGeoHash = geoHashService.generateGeoHash(latitude, longitude);
        List<String> searchGeoHashes = geoHashService.getNeighboringGeoHashes(locationGeoHash);
        
        Provider.ProviderType type = null;
        if (providerType != null) {
            try {
                type = Provider.ProviderType.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid provider type: {}", providerType);
            }
        }
        
        List<Vehicle> nearbyVehicles = vehicleRepository.findAvailableVehiclesByGeoHashes(
            searchGeoHashes, minCapacity != null ? minCapacity : 1, type
        );
        
        // Filter by actual distance if needed
        return nearbyVehicles.stream()
            .filter(vehicle -> {
                if (vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
                    return false;
                }
                double distance = geoHashService.calculateDistance(locationGeoHash, vehicle.getCurrentGeoHash());
                return distance <= SEARCH_RADIUS_KM;
            })
            .toList();
    }
    
    /**
     * Find providers serving a specific area using GeoHash
     */
    public List<Provider> getProvidersInArea(BigDecimal latitude, BigDecimal longitude, 
                                           String providerType) {
        String locationGeoHash = geoHashService.generateProviderGeoHash(latitude, longitude);
        List<String> searchGeoHashes = geoHashService.getNeighboringGeoHashes(locationGeoHash);
        
        Provider.ProviderType type = null;
        if (providerType != null) {
            try {
                type = Provider.ProviderType.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid provider type: {}", providerType);
            }
        }
        
        return providerRepository.findProvidersByGeoHashesAndType(searchGeoHashes, type);
    }
    
    /**
     * Search routes using GeoHash for efficient spatial queries
     */
    public List<Route> searchRoutesWithGeoHash(String startLocation, String endLocation, 
                                             BigDecimal startLat, BigDecimal startLon,
                                             BigDecimal endLat, BigDecimal endLon,
                                             String providerType) {
        
        List<String> startGeoHashes = List.of();
        List<String> endGeoHashes = List.of();
        
        if (startLat != null && startLon != null) {
            String startGeoHash = geoHashService.generateRouteGeoHash(startLat, startLon);
            startGeoHashes = geoHashService.getNeighboringGeoHashes(startGeoHash);
        }
        
        if (endLat != null && endLon != null) {
            String endGeoHash = geoHashService.generateRouteGeoHash(endLat, endLon);
            endGeoHashes = geoHashService.getNeighboringGeoHashes(endGeoHash);
        }
        
        Provider.ProviderType type = null;
        if (providerType != null) {
            try {
                type = Provider.ProviderType.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid provider type: {}", providerType);
            }
        }
        
        if (!startGeoHashes.isEmpty() && !endGeoHashes.isEmpty()) {
            return routeRepository.findRoutesByStartAndEndGeoHashes(startGeoHashes, endGeoHashes, type);
        } else {
            return routeRepository.findRoutesByLocationsAndProviderType(startLocation, endLocation, type);
        }
    }
    
    private List<Route> findOptimalRoutesWithGeoHash(BookingRequestDto request, 
                                                   String pickupGeoHash, String dropoffGeoHash) {
        List<String> startGeoHashes = geoHashService.getNeighboringGeoHashes(pickupGeoHash);
        List<String> endGeoHashes = geoHashService.getNeighboringGeoHashes(dropoffGeoHash);
        
        return routeRepository.findOptimalRoutesByGeoHashes(startGeoHashes, endGeoHashes);
    }
    
    private Optional<Vehicle> findNearestAvailableVehicle(String pickupGeoHash, Route route, 
                                                        Integer passengerCount) {
        List<String> searchGeoHashes = geoHashService.getNeighboringGeoHashes(pickupGeoHash);
        
        List<Vehicle> nearbyVehicles = vehicleRepository.findAvailableVehiclesByGeoHashes(
            searchGeoHashes, passengerCount, null
        );
        
        // Filter by provider and capacity
        return nearbyVehicles.stream()
            .filter(vehicle -> vehicle.getProvider().getId().equals(route.getProvider().getId()))
            .filter(vehicle -> vehicle.getCapacity() >= passengerCount)
            .findFirst();
    }
    
    private Trip createTripWithGeoHash(String bookingId, String userId, String phoneNumber, 
                                     Route route, Vehicle vehicle, BigDecimal totalFare, 
                                     BookingRequestDto request, String pickupGeoHash, 
                                     String dropoffGeoHash) {
        return Trip.builder()
            .bookingId(bookingId)
            .userId(userId)
            .phoneNumber(phoneNumber)
            .route(route)
            .vehicle(vehicle)
            .driver(vehicle.getDriver())
            .totalFare(totalFare)
            .passengerCount(request.getPassengerCount())
            .pickupLocation(request.getStartLocation())
            .dropoffLocation(request.getEndLocation())
            .pickupLatitude(request.getStartLatitude())
            .pickupLongitude(request.getStartLongitude())
            .pickupGeoHash(pickupGeoHash)
            .dropoffLatitude(request.getEndLatitude())
            .dropoffLongitude(request.getEndLongitude())
            .dropoffGeoHash(dropoffGeoHash)
            .scheduledPickupTime(request.getPreferredDateTime())
            .specialRequests(request.getSpecialRequests())
            .status(Trip.TripStatus.CONFIRMED)
            .estimatedArrivalTime(request.getPreferredDateTime().plusMinutes(
                routeOptimizationService.calculateEstimatedDuration(
                    route.getDistanceKm(), route.getProvider().getProviderType())))
            .build();
    }
    
    private BookingResponseDto createSuccessResponse(Trip trip) {
        return BookingResponseDto.builder()
            .tripId(trip.getId())
            .bookingId(trip.getBookingId())
            .status(trip.getStatus().name())
            .totalFare(trip.getTotalFare())
            .pickupLocation(trip.getPickupLocation())
            .dropoffLocation(trip.getDropoffLocation())
            .estimatedPickupTime(trip.getScheduledPickupTime())
            .estimatedArrivalTime(trip.getEstimatedArrivalTime())
            .vehicleId(trip.getVehicle().getId())
            .vehiclePlateNumber(trip.getVehicle().getPlateNumber())
            .vehicleModel(trip.getVehicle().getModel())
            .vehicleMake(trip.getVehicle().getMake())
            .driverId(trip.getDriver() != null ? trip.getDriver().getId() : null)
            .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
            .driverPhone(trip.getDriver() != null ? trip.getDriver().getPhoneNumber() : null)
            .driverRating(trip.getDriver() != null ? trip.getDriver().getRating() : null)
            .providerId(trip.getRoute().getProvider().getId())
            .providerName(trip.getRoute().getProvider().getName())
            .providerType(trip.getRoute().getProvider().getProviderType().name())
            .providerRating(trip.getRoute().getProvider().getRating())
            .routeId(trip.getRoute().getId())
            .distanceKm(trip.getRoute().getDistanceKm())
            .estimatedDurationMinutes(trip.getRoute().getEstimatedDurationMinutes())
            .message("Booking successful")
            .createdAt(trip.getCreatedAt())
            .build();
    }
    
    private BookingResponseDto createFailureResponse(String message) {
        return BookingResponseDto.builder()
            .status("FAILED")
            .message(message)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    private BookingProcessedEvent createBookingProcessedEvent(Trip trip, String status) {
        BookingProcessedEvent event = new BookingProcessedEvent();
        event.setEventType("booking-processed");
        event.setBookingId(trip.getBookingId());
        event.setUserId(trip.getUserId());
        event.setPhoneNumber(trip.getPhoneNumber());
        event.setStatus(status);
        event.setTripId(trip.getId());
        event.setRouteId(trip.getRoute().getId());
        event.setVehicleId(trip.getVehicle().getId());
        event.setProviderId(trip.getRoute().getProvider().getId());
        event.setTotalFare(trip.getTotalFare());
        event.setEstimatedPickupTime(trip.getScheduledPickupTime());
        event.setEstimatedArrivalTime(trip.getEstimatedArrivalTime());
        event.setPickupLocation(trip.getPickupLocation());
        event.setDropoffLocation(trip.getDropoffLocation());
        event.setVehiclePlateNumber(trip.getVehicle().getPlateNumber());
        event.setProviderName(trip.getRoute().getProvider().getName());
        
        if (trip.getDriver() != null) {
            event.setDriverId(trip.getDriver().getId());
            event.setDriverName(trip.getDriver().getFullName());
            event.setDriverPhone(trip.getDriver().getPhoneNumber());
        }
        
        event.setMessage("Booking processed successfully");
        event.setTimestamp(LocalDateTime.now());
        
        return event;
    }
    
    private String generateBookingId() {
        return "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}