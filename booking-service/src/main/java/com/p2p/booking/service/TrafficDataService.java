package com.p2p.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Traffic data service for real-time edge weight updates
 * Integrates with real-time location data from drivers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficDataService {
    
    private final Map<String, TrafficSegment> trafficData = new ConcurrentHashMap<>();
    private final Map<String, Double> historicalSpeeds = new ConcurrentHashMap<>();
    
    /**
     * Initialize historical speeds when application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeHistoricalSpeeds() {
        log.info("Initializing historical speeds data...");
        loadDefaultHistoricalSpeeds();
    }

    /**
     * Update historical speeds periodically based on recent traffic data
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void updateHistoricalSpeeds() {
        log.debug("Updating historical speeds based on recent traffic data...");

        // Calculate average speeds from recent traffic data
        trafficData.forEach((segmentId, segment) -> {
            if (segment.getLastUpdated().isAfter(LocalDateTime.now().minusHours(24))) {
                // Use recent data to update historical average
                double currentHistorical = historicalSpeeds.getOrDefault(segmentId, segment.getFreeFlowSpeed());
                double newAverage = (currentHistorical * 0.9) + (segment.getCurrentSpeed() * 0.1); // Weighted average
                historicalSpeeds.put(segmentId, newAverage);
            }
        });

        log.debug("Updated historical speeds for {} segments", historicalSpeeds.size());
    }

    /**
     * Load default historical speeds for common road segments
     */
    private void loadDefaultHistoricalSpeeds() {
        // Initialize with default speeds for different types of road segments
        // In a real system, this would load from a database or external service

        // City center segments (slower speeds)
        for (int i = 0; i < 100; i++) {
            String segmentId = "city_center_" + i;
            historicalSpeeds.put(segmentId, 25.0); // 25 km/h average
        }

        // Suburban segments (moderate speeds)
        for (int i = 0; i < 200; i++) {
            String segmentId = "suburban_" + i;
            historicalSpeeds.put(segmentId, 40.0); // 40 km/h average
        }

        // Highway segments (faster speeds)
        for (int i = 0; i < 50; i++) {
            String segmentId = "highway_" + i;
            historicalSpeeds.put(segmentId, 80.0); // 80 km/h average
        }

        // Partition-based segments for graph integration
        String[] partitionPrefixes = {"partition_0_0", "partition_0_1", "partition_1_0", "partition_1_1"};
        for (String prefix : partitionPrefixes) {
            for (int i = 0; i < 50; i++) {
                String segmentId = prefix + "_segment_" + i;
                // Vary speeds based on partition type
                double baseSpeed = 30.0 + (prefix.hashCode() % 20); // 30-50 km/h range
                historicalSpeeds.put(segmentId, baseSpeed);
            }
        }

        log.info("Loaded {} default historical speed entries", historicalSpeeds.size());
    }

    /**
     * Get current traffic weights for graph edges in specified partitions
     */
    @Cacheable(value = "trafficWeights", key = "#startPartition + '-' + #endPartition")
    public Map<String, Double> getCurrentTrafficWeights(String startPartition, String endPartition) {
        Map<String, Double> weights = new HashMap<>();
        
        // Get all traffic segments for the route partitions
        List<String> partitions = Arrays.asList(startPartition, endPartition);
        
        for (String partition : partitions) {
            trafficData.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(partition))
                .forEach(entry -> {
                    TrafficSegment segment = entry.getValue();
                    double travelTime = calculateTravelTime(segment);
                    weights.put(entry.getKey(), travelTime);
                });
        }
        
        // Fill in missing segments with historical data
        fillHistoricalWeights(weights, partitions);
        
        return weights;
    }
    
    /**
     * Update traffic data from real-time GPS data
     */
    @KafkaListener(topics = "driver-locations")
    public void updateTrafficFromDriverLocation(String locationData) {
        try {
            // Parse driver location update
            DriverLocationUpdate update = parseLocationUpdate(locationData);
            
            if (update != null) {
                updateTrafficSegment(update);
            }
            
        } catch (Exception e) {
            log.error("Error processing driver location update", e);
        }
    }
    
    /**
     * Predict traffic conditions based on historical patterns
     */
    public Map<String, Double> predictTrafficWeights(String partition, LocalDateTime futureTime) {
        Map<String, Double> predictions = new HashMap<>();
        
        // Use time-of-day patterns
        LocalTime timeOfDay = futureTime.toLocalTime();
        double timeFactor = getTimeOfDayFactor(timeOfDay);
        
        // Use day-of-week patterns
        double dayFactor = getDayOfWeekFactor(futureTime.getDayOfWeek());
        
        // Apply factors to historical speeds
        historicalSpeeds.forEach((segmentId, baseSpeed) -> {
            if (segmentId.startsWith(partition)) {
                double predictedSpeed = baseSpeed * timeFactor * dayFactor;
                double travelTime = calculateTravelTimeFromSpeed(segmentId, predictedSpeed);
                predictions.put(segmentId, travelTime);
            }
        });
        
        return predictions;
    }
    
    /**
     * Get traffic conditions summary for a partition
     */
    public TrafficConditions getTrafficConditions(String partition) {
        List<TrafficSegment> partitionSegments = trafficData.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(partition))
            .map(Map.Entry::getValue)
            .toList();
        
        if (partitionSegments.isEmpty()) {
            return new TrafficConditions(partition, "UNKNOWN", 1.0, LocalDateTime.now());
        }
        
        double avgSpeedRatio = partitionSegments.stream()
            .mapToDouble(segment -> segment.getCurrentSpeed() / segment.getFreeFlowSpeed())
            .average()
            .orElse(1.0);
        
        String condition = determineTrafficCondition(avgSpeedRatio);
        
        return new TrafficConditions(partition, condition, avgSpeedRatio, LocalDateTime.now());
    }
    
    /**
     * Calculate travel time for a traffic segment
     */
    private double calculateTravelTime(TrafficSegment segment) {
        double distance = segment.getDistanceKm();
        double currentSpeed = Math.max(segment.getCurrentSpeed(), 5.0); // Minimum 5 km/h
        
        // Travel time in minutes
        double baseTime = (distance / currentSpeed) * 60.0;
        
        // Add congestion penalty
        double congestionFactor = 1.0 + (segment.getCongestionLevel() * 0.5);
        
        return baseTime * congestionFactor;
    }
    
    /**
     * Update traffic segment with new driver data
     */
    private void updateTrafficSegment(DriverLocationUpdate update) {
        String segmentId = update.getSegmentId();
        
        TrafficSegment segment = trafficData.computeIfAbsent(segmentId, 
            id -> new TrafficSegment(id, update.getDistanceKm(), update.getFreeFlowSpeed()));
        
        // Update with new speed data
        segment.addSpeedSample(update.getCurrentSpeed(), update.getTimestamp());
        
        // Update congestion level based on speed ratio
        double speedRatio = update.getCurrentSpeed() / segment.getFreeFlowSpeed();
        segment.setCongestionLevel(calculateCongestionLevel(speedRatio));
        
        log.debug("Updated traffic segment {}: speed={} km/h, congestion={}", 
                 segmentId, update.getCurrentSpeed(), segment.getCongestionLevel());
    }
    
    /**
     * Fill missing weights with historical data
     */
    private void fillHistoricalWeights(Map<String, Double> weights, List<String> partitions) {
        LocalTime now = LocalTime.now();
        double timeFactor = getTimeOfDayFactor(now);
        
        for (String partition : partitions) {
            historicalSpeeds.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(partition))
                .filter(entry -> !weights.containsKey(entry.getKey()))
                .forEach(entry -> {
                    double adjustedSpeed = entry.getValue() * timeFactor;
                    double travelTime = calculateTravelTimeFromSpeed(entry.getKey(), adjustedSpeed);
                    weights.put(entry.getKey(), travelTime);
                });
        }
    }
    
    /**
     * Get time-of-day factor for traffic prediction
     */
    private double getTimeOfDayFactor(LocalTime time) {
        int hour = time.getHour();
        
        // Rush hour penalties
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
            return 0.6; // 40% slower during rush hour
        } else if (hour >= 22 || hour <= 5) {
            return 1.3; // 30% faster during night
        } else {
            return 1.0; // Normal speed
        }
    }
    
    /**
     * Get day-of-week factor for traffic prediction
     */
    private double getDayOfWeekFactor(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SATURDAY, SUNDAY -> 1.15; // 15% faster on weekends
            case FRIDAY -> 0.85; // 15% slower on Friday
            default -> 1.0;
        };
    }
    
    /**
     * Calculate travel time from speed and segment distance
     */
    private double calculateTravelTimeFromSpeed(String segmentId, double speedKmh) {
        // In a real system, segment distances would be stored
        double defaultDistance = 0.5; // 500m default segment
        return (defaultDistance / Math.max(speedKmh, 5.0)) * 60.0; // minutes
    }
    
    /**
     * Calculate congestion level from speed ratio
     */
    private double calculateCongestionLevel(double speedRatio) {
        if (speedRatio > 0.8) return 0.1; // Free flow
        if (speedRatio > 0.6) return 0.3; // Light congestion
        if (speedRatio > 0.4) return 0.6; // Moderate congestion
        if (speedRatio > 0.2) return 0.8; // Heavy congestion
        return 1.0; // Severe congestion
    }
    
    /**
     * Determine traffic condition from average speed ratio
     */
    private String determineTrafficCondition(double avgSpeedRatio) {
        if (avgSpeedRatio > 0.8) return "FREE_FLOW";
        if (avgSpeedRatio > 0.6) return "LIGHT_TRAFFIC";
        if (avgSpeedRatio > 0.4) return "MODERATE_TRAFFIC";
        if (avgSpeedRatio > 0.2) return "HEAVY_TRAFFIC";
        return "SEVERE_CONGESTION";
    }
    
    /**
     * Parse driver location update from Kafka message
     */
    private DriverLocationUpdate parseLocationUpdate(String locationData) {
        try {
            // In a real system, this would parse JSON from Kafka
            // For now, return a mock update
            return new DriverLocationUpdate(
                "segment_123",
                LocalDateTime.now(),
                25.0, // current speed
                40.0, // free flow speed
                0.5   // distance km
            );
        } catch (Exception e) {
            log.error("Error parsing location update: {}", locationData, e);
            return null;
        }
    }
    
    // Data classes
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class DriverLocationUpdate {
        private String segmentId;
        private LocalDateTime timestamp;
        private double currentSpeed;
        private double freeFlowSpeed;
        private double distanceKm;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TrafficConditions {
        private String partition;
        private String condition;
        private double speedRatio;
        private LocalDateTime timestamp;
    }
}

/**
 * Represents a traffic segment with real-time data
 */
@lombok.Data
class TrafficSegment {
    private final String id;
    private final double distanceKm;
    private final double freeFlowSpeed;
    private double currentSpeed;
    private double congestionLevel;
    private LocalDateTime lastUpdated;
    private final Queue<SpeedSample> speedSamples = new LinkedList<>();
    
    public TrafficSegment(String id, double distanceKm, double freeFlowSpeed) {
        this.id = id;
        this.distanceKm = distanceKm;
        this.freeFlowSpeed = freeFlowSpeed;
        this.currentSpeed = freeFlowSpeed;
        this.congestionLevel = 0.0;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void addSpeedSample(double speed, LocalDateTime timestamp) {
        speedSamples.offer(new SpeedSample(speed, timestamp));
        
        // Keep only last 10 samples
        while (speedSamples.size() > 10) {
            speedSamples.poll();
        }
        
        // Update current speed as moving average
        this.currentSpeed = speedSamples.stream()
            .mapToDouble(SpeedSample::getSpeed)
            .average()
            .orElse(speed);
        
        this.lastUpdated = timestamp;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class SpeedSample {
        private double speed;
        private LocalDateTime timestamp;
    }
}
