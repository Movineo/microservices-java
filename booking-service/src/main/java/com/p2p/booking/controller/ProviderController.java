package com.p2p.booking.controller;

import com.p2p.booking.entity.Provider;
import com.p2p.booking.entity.Vehicle;
import com.p2p.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/providers")
@CrossOrigin(origins = "*")
public class ProviderController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);
    
    @Autowired
    private BookingService bookingService;
    
    @GetMapping
    public ResponseEntity<List<Provider>> getProviders(
            @RequestParam(required = false) String providerType) {
        
        logger.info("Retrieving providers with type: {}", providerType);
        
        List<Provider> providers = bookingService.getAvailableProviders(providerType);
        return ResponseEntity.ok(providers);
    }
    
    @GetMapping("/vehicles/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles(
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Long providerId) {
        
        logger.info("Retrieving available vehicles with capacity >= {} for provider: {}", 
                   minCapacity, providerId);
        
        List<Vehicle> vehicles = bookingService.getAvailableVehicles(minCapacity, providerId);
        return ResponseEntity.ok(vehicles);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Provider Service is healthy");
    }
}