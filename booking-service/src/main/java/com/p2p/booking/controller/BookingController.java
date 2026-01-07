package com.p2p.booking.controller;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.dto.BookingResponseDto;
import com.p2p.booking.dto.ETARequest;
import com.p2p.booking.dto.ETAUpdateRequest;
import com.p2p.booking.entity.Trip;
import com.p2p.booking.service.BookingService;
import com.p2p.booking.service.RecommendationService;
import com.p2p.booking.service.ETACalculationService;
import com.p2p.booking.service.TrafficDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bookings/internal")
@CrossOrigin(origins = "*")
public class BookingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private RecommendationService recommendationService;
    
    @Autowired
    private ETACalculationService etaCalculationService;
    
    @Autowired
    private TrafficDataService trafficDataService;

    @PostMapping("/process")
    public ResponseEntity<BookingResponseDto> processBooking(
            @RequestHeader("User-Id") String userId,
            @RequestHeader("Phone-Number") String phoneNumber,
            @Valid @RequestBody BookingRequestDto request) {
        
        logger.info("Processing booking request for user: {}", userId);
        
        BookingResponseDto response = bookingService.processBooking(userId, phoneNumber, request);
        
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBookingDetails(@PathVariable String bookingId) {
        logger.info("Retrieving booking details for: {}", bookingId);
        
        Optional<BookingResponseDto> booking = bookingService.getBookingDetails(bookingId);

        return booking.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDto>> getUserBookings(@PathVariable String userId) {
        logger.info("Retrieving bookings for user: {}", userId);
        
        List<BookingResponseDto> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }
    
    @PutMapping("/{tripId}/status")
    public ResponseEntity<BookingResponseDto> updateTripStatus(
            @PathVariable Long tripId,
            @RequestParam Trip.TripStatus status) {
        
        logger.info("Updating trip {} status to: {}", tripId, status);
        
        BookingResponseDto response = bookingService.updateTripStatus(tripId, status);
        
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Booking Service is healthy");
    }

    @PostMapping("/recommendations/routes")
    public ResponseEntity<List<RecommendationService.RouteRecommendation>> getRouteRecommendations(
            @RequestHeader("User-Id") String userId,
            @Valid @RequestBody BookingRequestDto request) {

        logger.info("Getting route recommendations for user: {}", userId);
        List<RecommendationService.RouteRecommendation> recommendations =
            recommendationService.getRouteRecommendations(userId, request);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/recommendations/providers")
    public ResponseEntity<List<RecommendationService.ProviderRecommendation>> getProviderRecommendations(
            @RequestHeader("User-Id") String userId,
            @Valid @RequestBody BookingRequestDto request) {

        logger.info("Getting provider recommendations for user: {}", userId);
        List<RecommendationService.ProviderRecommendation> recommendations =
            recommendationService.getProviderRecommendations(userId, request);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/recommendations/times")
    public ResponseEntity<List<RecommendationService.TimeRecommendation>> getTimeRecommendations(
            @Valid @RequestBody BookingRequestDto request) {

        logger.info("Getting departure time recommendations");
        List<RecommendationService.TimeRecommendation> recommendations =
            recommendationService.getDepartureTimeRecommendations(request);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/eta/calculate")
    public ResponseEntity<ETACalculationService.ETAResult> calculateETA(
            @RequestBody ETARequest request) {
        
        logger.info("Calculating ETA from ({}, {}) to ({}, {})", 
                   request.getStartLat(), request.getStartLon(), 
                   request.getEndLat(), request.getEndLon());
        
        ETACalculationService.ETAResult eta = etaCalculationService.calculateETA(
            request.getStartLat(), request.getStartLon(),
            request.getEndLat(), request.getEndLon(),
            request.getDepartureTime()
        );
        
        return ResponseEntity.ok(eta);
    }
    
    @PostMapping("/eta/update")
    public ResponseEntity<ETACalculationService.ETAResult> updateETA(
            @RequestParam String tripId,
            @RequestBody ETAUpdateRequest request) {
        
        logger.info("Updating ETA for trip: {}", tripId);
        
        ETACalculationService.ETAResult updatedETA = etaCalculationService.updateETAWithProgress(
            tripId, request.getCurrentLat(), request.getCurrentLon(),
            request.getDestinationLat(), request.getDestinationLon()
        );
        
        return ResponseEntity.ok(updatedETA);
    }
    
    @GetMapping("/traffic/conditions/{partition}")
    public ResponseEntity<TrafficDataService.TrafficConditions> getTrafficConditions(
            @PathVariable String partition) {
        
        TrafficDataService.TrafficConditions conditions = 
            trafficDataService.getTrafficConditions(partition);
        return ResponseEntity.ok(conditions);
    }
}