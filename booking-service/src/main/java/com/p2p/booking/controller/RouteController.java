package com.p2p.booking.controller;

import com.p2p.booking.entity.Route;
import com.p2p.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routes")
@CrossOrigin(origins = "*")
public class RouteController {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);
    
    @Autowired
    private BookingService bookingService;
    
    @PostMapping("/search")
    public ResponseEntity<List<Route>> searchRoutes(
            @RequestParam String startLocation,
            @RequestParam String endLocation,
            @RequestParam(required = false) String providerType) {
        
        logger.info("Searching routes from {} to {} with provider type: {}", 
                   startLocation, endLocation, providerType);
        
        List<Route> routes = bookingService.searchRoutes(startLocation, endLocation, providerType);
        return ResponseEntity.ok(routes);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Route Service is healthy");
    }
}