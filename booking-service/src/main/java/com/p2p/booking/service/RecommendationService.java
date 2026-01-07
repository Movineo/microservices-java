package com.p2p.booking.service;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.entity.*;
import com.p2p.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final TripRepository tripRepository;
    private final ProviderRepository providerRepository;
    private final FareCalculationService fareCalculationService;
    private final RouteOptimizationService routeOptimizationService;
    
    /**
     * Get route recommendations based on user preferences and history
     */
    public List<RouteRecommendation> getRouteRecommendations(String userId, BookingRequestDto request) {
        log.info("Generating route recommendations for user: {}", userId);
        
        List<RouteRecommendation> recommendations = new ArrayList<>();
        
        // Get user's trip history for pattern analysis
        List<Trip> userHistory = tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Get base optimal routes
        List<Route> optimalRoutes = routeOptimizationService.findOptimalRoutes(request);
        
        for (Route route : optimalRoutes.subList(0, Math.min(5, optimalRoutes.size()))) {
            RouteRecommendation recommendation = createRouteRecommendation(route, request, userHistory);
            recommendations.add(recommendation);
        }
        
        // Add alternative time recommendations
        recommendations.addAll(getTimeBasedRecommendations(request, optimalRoutes));
        
        // Sort by recommendation score
        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));
        
        return recommendations.subList(0, Math.min(10, recommendations.size()));
    }
    
    /**
     * Get provider recommendations based on user history and ratings
     */
    public List<ProviderRecommendation> getProviderRecommendations(String userId, BookingRequestDto request) {
        log.info("Generating provider recommendations for user: {}", userId);
        
        List<Trip> userHistory = tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Get user's preferred providers based on history
        Map<Long, Integer> providerUsageCount = userHistory.stream()
            .collect(Collectors.groupingBy(
                trip -> trip.getRoute().getProvider().getId(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Get available providers for the route
        List<Provider> availableProviders = providerRepository.findByProviderTypeAndIsActiveTrue(
            Provider.ProviderType.valueOf(request.getProviderType().toUpperCase())
        );
        
        List<ProviderRecommendation> recommendations = new ArrayList<>();
        
        for (Provider provider : availableProviders) {
            ProviderRecommendation recommendation = createProviderRecommendation(
                provider, providerUsageCount.getOrDefault(provider.getId(), 0), userHistory
            );
            recommendations.add(recommendation);
        }
        
        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));
        
        return recommendations.subList(0, Math.min(5, recommendations.size()));
    }
    
    /**
     * Get optimal departure time recommendations
     */
    public List<TimeRecommendation> getDepartureTimeRecommendations(BookingRequestDto request) {
        log.info("Generating departure time recommendations");
        
        List<TimeRecommendation> recommendations = new ArrayList<>();
        LocalDateTime requestedTime = request.getPreferredDateTime();
        
        // Current time recommendation
        recommendations.add(createTimeRecommendation(requestedTime, "Requested Time", 1.0));
        
        // Off-peak recommendations (cheaper fares)
        LocalDateTime offPeak1 = findNextOffPeakTime(requestedTime, true);
        if (offPeak1 != null) {
            recommendations.add(createTimeRecommendation(offPeak1, "Earlier Off-Peak", 0.9));
        }
        
        LocalDateTime offPeak2 = findNextOffPeakTime(requestedTime, false);
        if (offPeak2 != null) {
            recommendations.add(createTimeRecommendation(offPeak2, "Later Off-Peak", 0.85));
        }
        
        // Early morning recommendation (less traffic)
        if (requestedTime.getHour() > 6) {
            LocalDateTime earlyMorning = requestedTime.withHour(6).withMinute(0);
            recommendations.add(createTimeRecommendation(earlyMorning, "Early Morning", 0.8));
        }
        
        return recommendations;
    }
    
    private RouteRecommendation createRouteRecommendation(Route route, BookingRequestDto request, List<Trip> userHistory) {
        RouteRecommendation recommendation = new RouteRecommendation();
        recommendation.setRoute(route);
        
        // Calculate estimated fare
        BigDecimal estimatedFare = fareCalculationService.calculateTotalFare(
            route, request.getPassengerCount(), request.getPreferredDateTime(), request.getSpecialRequests()
        );
        recommendation.setEstimatedFare(estimatedFare);
        
        // Calculate recommendation score based on multiple factors
        double score = calculateRouteScore(route, userHistory, estimatedFare);
        recommendation.setScore(score);
        
        // Set recommendation reason
        recommendation.setReason(generateRouteRecommendationReason(score));

        return recommendation;
    }
    
    private ProviderRecommendation createProviderRecommendation(Provider provider, int usageCount, List<Trip> userHistory) {
        ProviderRecommendation recommendation = new ProviderRecommendation();
        recommendation.setProvider(provider);
        recommendation.setUsageCount(usageCount);
        
        // Calculate average user rating for this provider
        double avgRating = userHistory.stream()
            .filter(trip -> trip.getRoute().getProvider().getId().equals(provider.getId()))
            .filter(trip -> trip.getRating() != null)
            .mapToDouble(trip -> trip.getRating().doubleValue())
            .average()
            .orElse(0.0);
        
        recommendation.setUserAverageRating(avgRating);
        
        // Calculate recommendation score
        double score = calculateProviderScore(provider, usageCount, avgRating);
        recommendation.setScore(score);
        
        recommendation.setReason(generateProviderRecommendationReason(provider, usageCount, avgRating));
        
        return recommendation;
    }
    
    private TimeRecommendation createTimeRecommendation(LocalDateTime time, String reason, double score) {
        TimeRecommendation recommendation = new TimeRecommendation();
        recommendation.setRecommendedTime(time);
        recommendation.setReason(reason);
        recommendation.setScore(score);
        recommendation.setPotentialSavings(calculatePotentialSavings(score));
        return recommendation;
    }
    
    private List<RouteRecommendation> getTimeBasedRecommendations(BookingRequestDto request, List<Route> routes) {
        List<RouteRecommendation> timeRecommendations = new ArrayList<>();
        
        if (!routes.isEmpty()) {
            Route bestRoute = routes.get(0);
            
            // Recommend earlier departure for same route
            LocalDateTime earlierTime = request.getPreferredDateTime().minusHours(1);
            if (earlierTime.isAfter(LocalDateTime.now())) {
                RouteRecommendation earlierRec = new RouteRecommendation();
                earlierRec.setRoute(bestRoute);
                earlierRec.setRecommendedDepartureTime(earlierTime);
                earlierRec.setReason("Depart 1 hour earlier for potentially lower fare");
                earlierRec.setScore(0.8);
                timeRecommendations.add(earlierRec);
            }
        }
        
        return timeRecommendations;
    }
    
    private double calculateRouteScore(Route route, List<Trip> userHistory, BigDecimal fare) {
        double score = 0.0;
        
        // Base score from route optimization
        BigDecimal providerRating = route.getProvider().getRating();
        score += (providerRating != null ? providerRating.doubleValue() : 3.0) * 0.3;

        // Fare score (lower fare = higher score)
        score += Math.max(0, (100 - fare.doubleValue()) / 100) * 0.3;
        
        // User history preference
        long providerUsage = userHistory.stream()
            .filter(trip -> trip.getRoute().getProvider().getId().equals(route.getProvider().getId()))
            .count();
        score += Math.min(providerUsage * 0.1, 0.4);
        
        return score;
    }
    
    private double calculateProviderScore(Provider provider, int usageCount, double avgRating) {
        double score = 0.0;
        
        // Provider rating
        BigDecimal providerRating = provider.getRating();
        score += (providerRating != null ? providerRating.doubleValue() : 3.0) * 0.4;

        // User's historical rating
        score += avgRating * 0.3;
        
        // Usage frequency
        score += Math.min(usageCount * 0.05, 0.3);
        
        return score;
    }
    
    private String generateRouteRecommendationReason(double score) {
        if (score > 4.0) return "Highly recommended based on rating and your preferences";
        if (score > 3.5) return "Good option with competitive fare";
        if (score > 3.0) return "Reliable provider with decent pricing";
        return "Alternative option worth considering";
    }
    
    private String generateProviderRecommendationReason(Provider provider, int usageCount, double avgRating) {
        if (usageCount > 5) return "You frequently choose this provider";
        if (avgRating > 4.0) return "You've rated this provider highly";
        BigDecimal providerRating = provider.getRating();
        if (providerRating != null && providerRating.compareTo(BigDecimal.valueOf(4.5)) > 0) {
            return "Highly rated by all users";
        }
        return "Worth trying based on overall ratings";
    }
    
    private LocalDateTime findNextOffPeakTime(LocalDateTime requestedTime, boolean earlier) {
        LocalTime time = requestedTime.toLocalTime();
        
        // Peak hours: 7-9 AM, 5-7 PM
        if (earlier) {
            if (time.isAfter(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(9, 0))) {
                return requestedTime.withHour(6).withMinute(30);
            }
            if (time.isAfter(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(19, 0))) {
                return requestedTime.withHour(16).withMinute(30);
            }
        } else {
            if (time.isAfter(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(9, 0))) {
                return requestedTime.withHour(9).withMinute(30);
            }
            if (time.isAfter(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(19, 0))) {
                return requestedTime.withHour(19).withMinute(30);
            }
        }
        
        return null;
    }
    
    private BigDecimal calculatePotentialSavings(double score) {
        // Estimate savings based on off-peak pricing
        if (score < 1.0) {
            return BigDecimal.valueOf((1.0 - score) * 50); // Up to 50 KES savings
        }
        return BigDecimal.ZERO;
    }
    
    // Inner classes for recommendation DTOs
    @Getter
    @Setter
    public static class RouteRecommendation {
        private Route route;
        private BigDecimal estimatedFare;
        private LocalDateTime recommendedDepartureTime;
        private String reason;
        private double score;
    }
    
    @Getter
    @Setter
    public static class ProviderRecommendation {
        private Provider provider;
        private int usageCount;
        private double userAverageRating;
        private String reason;
        private double score;
    }
    
    @Getter
    @Setter
    public static class TimeRecommendation {
        private LocalDateTime recommendedTime;
        private String reason;
        private double score;
        private BigDecimal potentialSavings;
    }
}
