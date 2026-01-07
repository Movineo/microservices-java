package com.p2p.booking.service;

import com.p2p.booking.entity.Route;
import com.p2p.booking.entity.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class FareCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FareCalculationService.class);
    
    // Base rates per km for different provider types in KES
    private static final BigDecimal MATATU_RATE_PER_KM = new BigDecimal("30");
    private static final BigDecimal BRT_RATE_PER_KM = new BigDecimal("25");
    private static final BigDecimal TAXI_RATE_PER_KM = new BigDecimal("50");
    private static final BigDecimal MOTORBIKE_RATE_PER_KM = new BigDecimal("40");
    private static final BigDecimal EV_RATE_PER_KM = new BigDecimal("35");
    
    // Surge pricing multipliers
    private static final BigDecimal PEAK_HOUR_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.2");
    private static final BigDecimal NIGHT_MULTIPLIER = new BigDecimal("1.3");
    
    public BigDecimal calculateTotalFare(Route route, int passengerCount, 
                                        LocalDateTime scheduledTime, String specialRequests) {
        logger.info("Calculating fare for route {} with {} passengers", 
                   route.getId(), passengerCount);
        
        BigDecimal baseFare = calculateBaseFare(route, passengerCount);
        BigDecimal surgeMultiplier = calculateSurgeMultiplier(scheduledTime);
        BigDecimal specialRequestsFee = calculateSpecialRequestsFee(specialRequests);
        
        BigDecimal totalFare = baseFare.multiply(surgeMultiplier).add(specialRequestsFee);
        
        // Apply minimum fare
        BigDecimal minimumFare = getMinimumFare(route.getProvider().getProviderType());
        if (totalFare.compareTo(minimumFare) < 0) {
            totalFare = minimumFare;
        }
        
        logger.info("Calculated total fare: {} KES", totalFare);
        return totalFare.setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calculateBaseFare(Route route, int passengerCount) {
        Provider.ProviderType providerType = route.getProvider().getProviderType();
        BigDecimal ratePerKm = getRatePerKm(providerType);
        
        BigDecimal distanceFare = route.getDistanceKm().multiply(ratePerKm);
        BigDecimal passengerMultiplier = new BigDecimal(Math.max(1, passengerCount));
        
        // For shared transport (Matatu, BRT), passenger count doesn't multiply the fare
        if (providerType == Provider.ProviderType.MATATU || 
            providerType == Provider.ProviderType.BRT) {
            return route.getBaseFare().add(distanceFare);
        }
        
        // For private transport (Taxi, Motorbike), apply passenger multiplier
        return route.getBaseFare().add(distanceFare.multiply(passengerMultiplier));
    }
    
    public BigDecimal calculateSurgeMultiplier(LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            return BigDecimal.ONE;
        }
        
        BigDecimal multiplier = BigDecimal.ONE;
        LocalTime time = scheduledTime.toLocalTime();
        
        // Peak hours: 7-9 AM and 5-7 PM
        if ((time.isAfter(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(9, 0))) ||
            (time.isAfter(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(19, 0)))) {
            multiplier = multiplier.multiply(PEAK_HOUR_MULTIPLIER);
        }
        
        // Night hours: 10 PM - 6 AM
        if (time.isAfter(LocalTime.of(22, 0)) || time.isBefore(LocalTime.of(6, 0))) {
            multiplier = multiplier.multiply(NIGHT_MULTIPLIER);
        }
        
        // Weekend multiplier
        int dayOfWeek = scheduledTime.getDayOfWeek().getValue();
        if (dayOfWeek == 6 || dayOfWeek == 7) { // Saturday or Sunday
            multiplier = multiplier.multiply(WEEKEND_MULTIPLIER);
        }
        
        return multiplier;
    }
    
    public BigDecimal calculateSpecialRequestsFee(String specialRequests) {
        if (specialRequests == null || specialRequests.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal fee = BigDecimal.ZERO;
        String requests = specialRequests.toLowerCase();
        
        // Extra luggage
        if (requests.contains("luggage") || requests.contains("baggage")) {
            fee = fee.add(new BigDecimal("100")); // 100 KES
        }
        
        // Child seat
        if (requests.contains("child seat") || requests.contains("baby seat")) {
            fee = fee.add(new BigDecimal("150")); // 150 KES
        }
        
        // Wheelchair accessible
        if (requests.contains("wheelchair") || requests.contains("accessible")) {
            fee = fee.add(new BigDecimal("200")); // 200 KES
        }
        
        // Pet transport
        if (requests.contains("pet") || requests.contains("animal")) {
            fee = fee.add(new BigDecimal("300")); // 300 KES
        }
        
        // Express/Priority
        if (requests.contains("express") || requests.contains("priority") || requests.contains("urgent")) {
            fee = fee.add(new BigDecimal("500")); // 500 KES
        }
        
        return fee;
    }
    
    private BigDecimal getRatePerKm(Provider.ProviderType providerType) {
        switch (providerType) {
            case MATATU:
                return MATATU_RATE_PER_KM;
            case BRT:
                return BRT_RATE_PER_KM;
            case TAXI:
                return TAXI_RATE_PER_KM;
            case MOTORBIKE:
                return MOTORBIKE_RATE_PER_KM;
            case EV:
                return EV_RATE_PER_KM;
            default:
                return MATATU_RATE_PER_KM;
        }
    }
    
    private BigDecimal getMinimumFare(Provider.ProviderType providerType) {
        switch (providerType) {
            case MATATU:
                return new BigDecimal("50"); // 50 KES minimum
            case BRT:
                return new BigDecimal("30"); // 30 KES minimum
            case TAXI:
                return new BigDecimal("200"); // 200 KES minimum
            case MOTORBIKE:
                return new BigDecimal("100"); // 100 KES minimum
            case EV:
                return new BigDecimal("150"); // 150 KES minimum
            default:
                return new BigDecimal("50");
        }
    }
    
    public BigDecimal calculateEstimatedFareRange(BigDecimal distance, Provider.ProviderType providerType, 
                                                 int passengerCount, boolean includeSurge) {
        BigDecimal ratePerKm = getRatePerKm(providerType);
        BigDecimal baseFare = distance.multiply(ratePerKm);
        
        if (providerType != Provider.ProviderType.MATATU && 
            providerType != Provider.ProviderType.BRT) {
            baseFare = baseFare.multiply(new BigDecimal(passengerCount));
        }
        
        if (includeSurge) {
            baseFare = baseFare.multiply(PEAK_HOUR_MULTIPLIER);
        }
        
        BigDecimal minimumFare = getMinimumFare(providerType);
        return baseFare.max(minimumFare).setScale(2, RoundingMode.HALF_UP);
    }
}