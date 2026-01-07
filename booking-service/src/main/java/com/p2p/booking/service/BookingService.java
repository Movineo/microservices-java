package com.p2p.booking.service;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.dto.BookingResponseDto;
import com.p2p.booking.entity.*;
import com.p2p.booking.events.BookingProcessedEvent;
import com.p2p.booking.events.TripStartedEvent;
import com.p2p.booking.events.TripCompletedEvent;
import com.p2p.booking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    
    @Autowired
    private TripRepository tripRepository;
    
    @Autowired
    private RouteRepository routeRepository;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private DriverRepository driverRepository;
    
    @Autowired
    private ProviderRepository providerRepository;
    
    @Autowired
    private RouteOptimizationService routeOptimizationService;
    
    @Autowired
    private FareCalculationService fareCalculationService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public BookingResponseDto processBooking(String userId, String phoneNumber, 
                                           BookingRequestDto request) {
        logger.info("Processing booking request for user: {}", userId);
        
        try {
            // Find optimal route
            List<Route> optimalRoutes = routeOptimizationService.findOptimalRoutes(request);
            if (optimalRoutes.isEmpty()) {
                return createFailureResponse("No routes available for the requested journey");
            }
            
            Route selectedRoute = optimalRoutes.get(0);
            
            // Find available vehicle
            Optional<Vehicle> availableVehicle = findAvailableVehicle(selectedRoute, request.getPassengerCount());
            if (availableVehicle.isEmpty()) {
                return createFailureResponse("No available vehicles for this route");
            }
            
            Vehicle vehicle = availableVehicle.get();
            
            // Calculate fare
            BigDecimal totalFare = fareCalculationService.calculateTotalFare(
                selectedRoute, 
                request.getPassengerCount(), 
                request.getPreferredDateTime(), 
                request.getSpecialRequests()
            );
            
            // Create trip
            String bookingId = generateBookingId();
            Trip trip = createTrip(bookingId, userId, phoneNumber, selectedRoute, vehicle, 
                                 totalFare, request);
            
            Trip savedTrip = tripRepository.save(trip);
            
            // Update vehicle status
            vehicle.setStatus(Vehicle.VehicleStatus.IN_TRANSIT);
            vehicleRepository.save(vehicle);
            
            // Create and publish booking processed event
            BookingProcessedEvent event = createBookingProcessedEvent(savedTrip, "CONFIRMED");
            kafkaTemplate.send("booking-events", event);
            
            logger.info("Booking processed successfully: {}", bookingId);
            return createSuccessResponse(savedTrip);
            
        } catch (Exception e) {
            logger.error("Error processing booking: {}", e.getMessage(), e);
            return createFailureResponse("Failed to process booking: " + e.getMessage());
        }
    }
    
    public Optional<BookingResponseDto> getBookingDetails(String bookingId) {
        Optional<Trip> trip = tripRepository.findByBookingId(bookingId);
        return trip.map(this::createSuccessResponse);
    }
    
    public List<BookingResponseDto> getUserBookings(String userId) {
        List<Trip> trips = tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return trips.stream()
                   .map(this::createSuccessResponse)
                   .toList();
    }
    
    public BookingResponseDto updateTripStatus(Long tripId, Trip.TripStatus newStatus) {
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            return createFailureResponse("Trip not found");
        }
        
        Trip trip = tripOpt.get();
        Trip.TripStatus oldStatus = trip.getStatus();
        trip.setStatus(newStatus);
        
        // Update timestamps based on status
        switch (newStatus) {
            case IN_PROGRESS:
                trip.setActualPickupTime(LocalDateTime.now());
                publishTripStartedEvent(trip);
                break;
            case COMPLETED:
                trip.setActualArrivalTime(LocalDateTime.now());
                // Update vehicle status
                trip.getVehicle().setStatus(Vehicle.VehicleStatus.AVAILABLE);
                vehicleRepository.save(trip.getVehicle());
                publishTripCompletedEvent(trip);
                break;
            case CANCELLED:
                // Update vehicle status
                trip.getVehicle().setStatus(Vehicle.VehicleStatus.AVAILABLE);
                vehicleRepository.save(trip.getVehicle());
                break;
        }
        
        Trip savedTrip = tripRepository.save(trip);
        logger.info("Trip {} status updated from {} to {}", tripId, oldStatus, newStatus);
        
        return createSuccessResponse(savedTrip);
    }
    
    public List<Route> searchRoutes(String startLocation, String endLocation, String providerType) {
        Provider.ProviderType type = null;
        if (providerType != null) {
            try {
                type = Provider.ProviderType.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid provider type: {}", providerType);
            }
        }
        
        return routeRepository.findRoutesByLocationsAndProviderType(startLocation, endLocation, type);
    }
    
    public List<Provider> getAvailableProviders(String providerType) {
        Provider.ProviderType type = null;
        if (providerType != null) {
            try {
                type = Provider.ProviderType.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid provider type: {}", providerType);
                return providerRepository.findByIsActiveTrue();
            }
        }
        
        if (type != null) {
            return providerRepository.findByProviderTypeAndIsActiveTrue(type);
        }
        
        return providerRepository.findByIsActiveTrue();
    }
    
    public List<Vehicle> getAvailableVehicles(Integer minCapacity, Long providerId) {
        return vehicleRepository.findAvailableVehicles(minCapacity != null ? minCapacity : 1, providerId);
    }
    
    private Optional<Vehicle> findAvailableVehicle(Route route, int passengerCount) {
        List<Vehicle> availableVehicles = vehicleRepository.findAvailableVehicles(passengerCount, route.getProvider().getId());
        return availableVehicles.stream()
                               .filter(vehicle -> vehicle.getCapacity() >= passengerCount)
                               .findFirst();
    }
    
    private Trip createTrip(String bookingId, String userId, String phoneNumber, 
                          Route route, Vehicle vehicle, BigDecimal totalFare, 
                          BookingRequestDto request) {
        Trip trip = new Trip();
        trip.setBookingId(bookingId);
        trip.setUserId(userId);
        trip.setPhoneNumber(phoneNumber);
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(vehicle.getDriver());
        trip.setTotalFare(totalFare);
        trip.setPassengerCount(request.getPassengerCount());
        trip.setPickupLocation(request.getStartLocation());
        trip.setDropoffLocation(request.getEndLocation());
        trip.setPickupLatitude(request.getStartLatitude());
        trip.setPickupLongitude(request.getStartLongitude());
        trip.setDropoffLatitude(request.getEndLatitude());
        trip.setDropoffLongitude(request.getEndLongitude());
        trip.setScheduledPickupTime(request.getPreferredDateTime());
        trip.setSpecialRequests(request.getSpecialRequests());
        trip.setStatus(Trip.TripStatus.CONFIRMED);
        
        // Calculate estimated arrival time
        int durationMinutes = routeOptimizationService.calculateEstimatedDuration(
            route.getDistanceKm(), route.getProvider().getProviderType());
        trip.setEstimatedArrivalTime(request.getPreferredDateTime().plusMinutes(durationMinutes));
        
        return trip;
    }
    
    private BookingResponseDto createSuccessResponse(Trip trip) {
        BookingResponseDto response = new BookingResponseDto();
        response.setTripId(trip.getId());
        response.setBookingId(trip.getBookingId());
        response.setStatus(trip.getStatus().name());
        response.setTotalFare(trip.getTotalFare());
        response.setPickupLocation(trip.getPickupLocation());
        response.setDropoffLocation(trip.getDropoffLocation());
        response.setEstimatedPickupTime(trip.getScheduledPickupTime());
        response.setEstimatedArrivalTime(trip.getEstimatedArrivalTime());
        
        // Vehicle information
        Vehicle vehicle = trip.getVehicle();
        if (vehicle != null) {
            response.setVehicleId(vehicle.getId());
            response.setVehiclePlateNumber(vehicle.getPlateNumber());
            response.setVehicleModel(vehicle.getModel());
            response.setVehicleMake(vehicle.getMake());
        }
        
        // Driver information
        Driver driver = trip.getDriver();
        if (driver != null) {
            response.setDriverId(driver.getId());
            response.setDriverName(driver.getFullName());
            response.setDriverPhone(driver.getPhoneNumber());
            response.setDriverRating(driver.getRating());
        }
        
        // Provider information
        Provider provider = trip.getRoute().getProvider();
        if (provider != null) {
            response.setProviderId(provider.getId());
            response.setProviderName(provider.getName());
            response.setProviderType(provider.getProviderType().name());
            response.setProviderRating(provider.getRating());
        }
        
        // Route information
        Route route = trip.getRoute();
        if (route != null) {
            response.setRouteId(route.getId());
            response.setDistanceKm(route.getDistanceKm());
            response.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
        }
        
        response.setMessage("Booking successful");
        response.setCreatedAt(trip.getCreatedAt());
        
        return response;
    }
    
    private BookingResponseDto createFailureResponse(String message) {
        BookingResponseDto response = new BookingResponseDto();
        response.setStatus("FAILED");
        response.setMessage(message);
        response.setCreatedAt(LocalDateTime.now());
        return response;
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
    
    private void publishTripStartedEvent(Trip trip) {
        TripStartedEvent event = new TripStartedEvent(
            "trip-started",
            trip.getId(),
            trip.getBookingId(),
            trip.getUserId(),
            trip.getPhoneNumber(),
            trip.getDriver() != null ? trip.getDriver().getId() : null,
            trip.getVehicle().getId(),
            trip.getPickupLocation(),
            trip.getDropoffLocation()
        );
        
        kafkaTemplate.send("trip-events", event);
    }
    
    private void publishTripCompletedEvent(Trip trip) {
        TripCompletedEvent event = new TripCompletedEvent(
            "trip-completed",
            trip.getId(),
            trip.getBookingId(),
            trip.getUserId(),
            trip.getPhoneNumber(),
            trip.getDriver() != null ? trip.getDriver().getId() : null,
            trip.getVehicle().getId(),
            trip.getRoute().getProvider().getId(),
            trip.getTotalFare()
        );
        
        event.setPickupLocation(trip.getPickupLocation());
        event.setDropoffLocation(trip.getDropoffLocation());
        event.setStartedAt(trip.getActualPickupTime());
        event.setCompletedAt(trip.getActualArrivalTime());
        
        kafkaTemplate.send("trip-events", event);
    }
    
    private String generateBookingId() {
        return "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}