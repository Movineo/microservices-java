package com.p2p.booking.controller;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.dto.BookingResponseDto;
import com.p2p.booking.entity.Provider;
import com.p2p.booking.entity.Route;
import com.p2p.booking.entity.Vehicle;
import com.p2p.booking.service.EnhancedBookingService;
import com.p2p.booking.service.GeoHashIndexingService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class EnhancedBookingController {
    
    private final EnhancedBookingService enhancedBookingService;
    private final GeoHashIndexingService geoHashIndexingService;
    
    /**
     * Create a new booking with GeoHash optimization
     */
    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @RequestHeader("User-Id") String userId,
            @RequestHeader("Phone-Number") String phoneNumber,
            @Valid @RequestBody BookingRequestDto request) {
        
        log.info("Creating booking for user: {}", userId);
        
        BookingResponseDto response = enhancedBookingService.processBookingWithGeoHash(
            userId, phoneNumber, request
        );
        
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Search available routes with GeoHash optimization
     */
    @PostMapping("/routes/search")
    public ResponseEntity<List<Route>> searchRoutes(
            @RequestParam String startLocation,
            @RequestParam String endLocation,
            @RequestParam(required = false) BigDecimal startLatitude,
            @RequestParam(required = false) BigDecimal startLongitude,
            @RequestParam(required = false) BigDecimal endLatitude,
            @RequestParam(required = false) BigDecimal endLongitude,
            @RequestParam(required = false) String providerType) {
        
        log.info("Searching GeoHash-optimized routes from {} to {}", startLocation, endLocation);
        
        List<Route> routes = enhancedBookingService.searchRoutesWithGeoHash(
            startLocation, endLocation,
            startLatitude, startLongitude,
            endLatitude, endLongitude,
            providerType
        );
        
        return ResponseEntity.ok(routes);
    }
    
    /**
     * Get available vehicles near a location using GeoHash
     */
    @GetMapping("/vehicles/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String providerType) {
        
        log.info("Finding available vehicles near location: {}, {}", latitude, longitude);
        
        List<Vehicle> vehicles = enhancedBookingService.getAvailableVehiclesNearLocation(
            latitude, longitude, minCapacity, providerType
        );
        
        return ResponseEntity.ok(vehicles);
    }
    
    /**
     * Get providers serving a specific area
     */
    @GetMapping("/providers")
    public ResponseEntity<List<Provider>> getProvidersInArea(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(required = false) String providerType) {
        
        log.info("Finding providers in area: {}, {}", latitude, longitude);
        
        List<Provider> providers = enhancedBookingService.getProvidersInArea(
            latitude, longitude, providerType
        );
        
        return ResponseEntity.ok(providers);
    }
    
    /**
     * Update vehicle location (for real-time tracking)
     */
    @PutMapping("/vehicles/{vehicleId}/location")
    public ResponseEntity<String> updateVehicleLocation(
            @PathVariable Long vehicleId,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        
        log.info("Updating location for vehicle {}: {}, {}", vehicleId, latitude, longitude);
        
        geoHashIndexingService.updateVehicleLocation(vehicleId, latitude, longitude);
        
        return ResponseEntity.ok("Vehicle location updated successfully");
    }
    
    /**
     * Rebuild all GeoHash indexes (admin endpoint)
     */
    @PostMapping("/admin/rebuild-geohash-indexes")
    public ResponseEntity<String> rebuildGeoHashIndexes() {
        log.info("Rebuilding GeoHash indexes");
        
        geoHashIndexingService.rebuildAllGeoHashIndexes();
        
        return ResponseEntity.ok("GeoHash indexes rebuilt successfully");
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Enhanced Booking Service is healthy");
    }
}