package com.p2p.booking.service;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.entity.Route;
import com.p2p.booking.entity.Provider;
import com.p2p.booking.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class RouteOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteOptimizationService.class);
    
    @Autowired
    private RouteRepository routeRepository;
    
    public List<Route> findOptimalRoutes(BookingRequestDto request) {
        logger.info("Finding optimal routes from {} to {}", 
                   request.getStartLocation(), request.getEndLocation());
        
        Provider.ProviderType providerType = null;
        if (request.getProviderType() != null) {
            try {
                providerType = Provider.ProviderType.valueOf(request.getProviderType().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid provider type: {}", request.getProviderType());
            }
        }
        
        List<Route> routes = routeRepository.findRoutesByLocationsAndProviderType(
            request.getStartLocation(), 
            request.getEndLocation(), 
            providerType
        );
        
        // Sort routes by a combination of distance, fare, and provider rating
        routes.sort((r1, r2) -> {
            double score1 = calculateRouteScore(r1);
            double score2 = calculateRouteScore(r2);
            return Double.compare(score1, score2);
        });
        
        logger.info("Found {} optimal routes", routes.size());
        return routes;
    }
    
    public BigDecimal calculateFare(Route route, int passengerCount) {
        if (route == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal baseFare = route.getBaseFare();
        BigDecimal distanceFare = route.getDistanceKm().multiply(new BigDecimal("50")); // 50 KES per km
        BigDecimal passengerMultiplier = new BigDecimal(passengerCount);
        
        // Calculate total fare considering distance and passenger count
        BigDecimal totalFare = baseFare.add(distanceFare).multiply(passengerMultiplier);
        
        // Apply provider type multiplier
        BigDecimal providerMultiplier = getProviderTypeMultiplier(route.getProvider().getProviderType());
        totalFare = totalFare.multiply(providerMultiplier);
        
        // Round to 2 decimal places
        return totalFare.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calculateDistance(BigDecimal startLat, BigDecimal startLon, 
                                      BigDecimal endLat, BigDecimal endLon) {
        if (startLat == null || startLon == null || endLat == null || endLon == null) {
            return BigDecimal.ZERO;
        }
        
        // Haversine formula to calculate distance between two points
        double lat1Rad = Math.toRadians(startLat.doubleValue());
        double lon1Rad = Math.toRadians(startLon.doubleValue());
        double lat2Rad = Math.toRadians(endLat.doubleValue());
        double lon2Rad = Math.toRadians(endLon.doubleValue());
        
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = 6371 * c; // Earth's radius in kilometers
        
        return new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP);
    }
    
    public int calculateEstimatedDuration(BigDecimal distanceKm, Provider.ProviderType providerType) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        // Average speed by provider type (km/h)
        double averageSpeed = switch (providerType) {
            case MATATU -> 40.0; // Matatus average 40 km/h in city traffic
            case BRT -> 35.0; // BRT with stops
            case TAXI -> 45.0; // Taxis can navigate better
            case MOTORBIKE -> 50.0; // Motorbikes can weave through traffic
            case EV -> 40.0; // Similar to matatus
            default -> 40.0;
        };

        double hours = distanceKm.doubleValue() / averageSpeed;
        int minutes = (int) Math.ceil(hours * 60);
        
        // Add buffer time based on distance
        if (distanceKm.compareTo(new BigDecimal("10")) > 0) {
            minutes += 15; // 15 minutes buffer for longer trips
        } else {
            minutes += 5; // 5 minutes buffer for shorter trips
        }
        
        return minutes;
    }
    
    private double calculateRouteScore(Route route) {
        // Lower score = better route
        double distanceScore = route.getDistanceKm().doubleValue() * 0.3;
        double fareScore = route.getBaseFare().doubleValue() * 0.0001; // Normalize fare
        
        // Provider rating bonus (higher rating = lower score)
        double ratingBonus = 0;
        if (route.getProvider().getRating() != null) {
            ratingBonus = (5.0 - route.getProvider().getRating().doubleValue()) * 0.2;
        }
        
        return distanceScore + fareScore + ratingBonus;
    }
    
    private BigDecimal getProviderTypeMultiplier(Provider.ProviderType providerType) {
        switch (providerType) {
            case MATATU:
                return new BigDecimal("1.0"); // Base multiplier
            case BRT:
                return new BigDecimal("0.8"); // Subsidized, cheaper
            case TAXI:
                return new BigDecimal("1.5"); // Premium service
            case MOTORBIKE:
                return new BigDecimal("0.7"); // Cheaper option
            case EV:
                return new BigDecimal("1.2"); // Slightly premium for eco-friendly
            default:
                return new BigDecimal("1.0");
        }
    }
}