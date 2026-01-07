package com.p2p.booking.controller;

import com.p2p.booking.entity.Route;
import com.p2p.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GraphQLController {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphQLController.class);
    
    @Autowired
    private BookingService bookingService;
    
    @QueryMapping
    public List<Route> routes(@Argument String startLocation, 
                             @Argument String endLocation,
                             @Argument String providerType) {
        
        logger.info("GraphQL query for routes from {} to {} with provider type: {}", 
                   startLocation, endLocation, providerType);
        
        return bookingService.searchRoutes(startLocation, endLocation, providerType);
    }
    
    @QueryMapping
    public List<com.p2p.booking.entity.Provider> providers(@Argument String providerType) {
        logger.info("GraphQL query for providers with type: {}", providerType);
        return bookingService.getAvailableProviders(providerType);
    }
    
    @QueryMapping
    public List<com.p2p.booking.entity.Vehicle> availableVehicles(@Argument Integer minCapacity, 
                                                                  @Argument Long providerId) {
        logger.info("GraphQL query for available vehicles with capacity >= {} for provider: {}", 
                   minCapacity, providerId);
        return bookingService.getAvailableVehicles(minCapacity, providerId);
    }
}