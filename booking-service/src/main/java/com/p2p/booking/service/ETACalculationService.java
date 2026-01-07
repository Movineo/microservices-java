package com.p2p.booking.service;

import com.p2p.booking.entity.*;
import com.p2p.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Enhanced ETA calculation service using graph-based algorithms
 * similar to Uber's approach
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ETACalculationService {
    
    private final GraphService graphService;
    private final TrafficDataService trafficDataService;
    private final MapMatchingService mapMatchingService;
    private final GeoHashService geoHashService;
    
    /**
     * Calculate ETA using graph-based shortest path with real-time traffic
     */
    public ETAResult calculateETA(BigDecimal startLat, BigDecimal startLon, 
                                 BigDecimal endLat, BigDecimal endLon,
                                 LocalDateTime departureTime) {
        
        log.info("Calculating ETA from ({}, {}) to ({}, {})", startLat, startLon, endLat, endLon);
        
        try {
            // Convert coordinates to graph vertices
            GraphVertex startVertex = graphService.findNearestVertex(startLat, startLon);
            GraphVertex endVertex = graphService.findNearestVertex(endLat, endLon);
            
            // Get real-time traffic data for edge weights
            Map<String, Double> trafficWeights = trafficDataService.getCurrentTrafficWeights(
                startVertex.getPartition(), endVertex.getPartition()
            );
            
            // Find optimal path using partitioned graph
            ETAPathResult pathResult = findOptimalPath(startVertex, endVertex, trafficWeights);
            
            // Calculate total ETA
            double totalTimeMinutes = pathResult.getTotalWeight();
            
            return ETAResult.builder()
                .estimatedTimeMinutes(totalTimeMinutes)
                .estimatedArrival(departureTime.plusMinutes((long) totalTimeMinutes))
                .path(pathResult.getPath())
                .distance(pathResult.getDistance())
                .confidence(calculateConfidence(pathResult))
                .alternativeRoutes(findAlternativeRoutes(startVertex, endVertex, trafficWeights))
                .build();
                
        } catch (Exception e) {
            log.error("Error calculating ETA", e);
            return fallbackETA(startLat, startLon, endLat, endLon, departureTime);
        }
    }
    
    /**
     * Update ETA in real-time as driver progresses
     */
    public ETAResult updateETAWithProgress(String tripId, BigDecimal currentLat, BigDecimal currentLon,
                                          BigDecimal destinationLat, BigDecimal destinationLon) {
        
        // Use map matching to get accurate position
        GraphVertex currentVertex = mapMatchingService.matchToRoad(currentLat, currentLon, tripId);
        GraphVertex destinationVertex = graphService.findNearestVertex(destinationLat, destinationLon);
        
        // Get current traffic conditions
        Map<String, Double> currentTraffic = trafficDataService.getCurrentTrafficWeights(
            currentVertex.getPartition(), destinationVertex.getPartition()
        );
        
        // Recalculate remaining journey
        ETAPathResult remainingPath = findOptimalPath(currentVertex, destinationVertex, currentTraffic);
        
        return ETAResult.builder()
            .estimatedTimeMinutes(remainingPath.getTotalWeight())
            .estimatedArrival(LocalDateTime.now().plusMinutes((long) remainingPath.getTotalWeight()))
            .path(remainingPath.getPath())
            .distance(remainingPath.getDistance())
            .confidence(calculateConfidence(remainingPath))
            .build();
    }
    
    /**
     * Find optimal path using partitioned graph approach
     */
    private ETAPathResult findOptimalPath(GraphVertex start, GraphVertex end, 
                                         Map<String, Double> trafficWeights) {
        
        // Check if both vertices are in same partition
        if (start.getPartition().equals(end.getPartition())) {
            return findIntraPartitionPathInternal(start, end, trafficWeights);
        } else {
            return findCrossPartitionPath(start, end, trafficWeights);
        }
    }
    
    /**
     * Handle paths within the same partition using pre-computed routes
     */
    @Cacheable(value = "intraPartitionPaths", key = "#start.id + '-' + #end.id")
    public ETAPathResult findIntraPartitionPath(GraphVertex start, GraphVertex end, 
                                               Map<String, Double> trafficWeights) {
        return findIntraPartitionPathInternal(start, end, trafficWeights);
    }

    private ETAPathResult findIntraPartitionPathInternal(GraphVertex start, GraphVertex end,
                                                        Map<String, Double> trafficWeights) {
        // Check pre-computed paths cache
        String cacheKey = start.getId() + "-" + end.getId();
        PathResult precomputed = graphService.getPrecomputedPath(cacheKey);
        
        if (precomputed != null) {
            // Adjust for current traffic conditions
            return convertToETAPathResult(adjustPathForTraffic(precomputed, trafficWeights));
        }
        
        // Fallback to modified Dijkstra for small partition
        PathResult result = graphService.dijkstraWithinPartition(start, end, trafficWeights);
        return convertToETAPathResult(result);
    }
    
    /**
     * Handle cross-partition paths using meta-graph
     */
    private ETAPathResult findCrossPartitionPath(GraphVertex start, GraphVertex end, 
                                                Map<String, Double> trafficWeights) {
        
        List<PathSegment> segments = new ArrayList<>();
        
        // 1. Path from start to partition boundary
        GraphVertex startBoundary = graphService.findBestBoundaryVertex(start.getPartition(), end.getPartition());
        ETAPathResult startSegment = findIntraPartitionPathInternal(start, startBoundary, trafficWeights);
        segments.add(new PathSegment(startSegment, start.getPartition()));
        
        // 2. Cross-partition path using meta-graph
        List<String> partitionPath = graphService.findPartitionPath(start.getPartition(), end.getPartition());
        for (int i = 0; i < partitionPath.size() - 1; i++) {
            PathResult crossSegment = graphService.getCrossPartitionPath(partitionPath.get(i), partitionPath.get(i + 1));
            segments.add(new PathSegment(convertToETAPathResult(crossSegment), partitionPath.get(i + 1)));
        }
        
        // 3. Path from boundary to destination
        GraphVertex endBoundary = graphService.findBestBoundaryVertex(end.getPartition(), start.getPartition());
        ETAPathResult endSegment = findIntraPartitionPathInternal(endBoundary, end, trafficWeights);
        segments.add(new PathSegment(endSegment, end.getPartition()));
        
        return combinePathSegments(segments);
    }
    
    /**
     * Convert PathResult to ETAPathResult
     */
    private ETAPathResult convertToETAPathResult(PathResult pathResult) {
        return ETAPathResult.builder()
            .path(pathResult.getPath())
            .totalWeight(pathResult.getTotalWeight())
            .distance(pathResult.getDistance())
            .build();
    }
    
    /**
     * Adjust pre-computed path weights based on current traffic
     */
    private PathResult adjustPathForTraffic(PathResult precomputed, Map<String, Double> trafficWeights) {
        double adjustedWeight = 0.0;
        List<GraphVertex> adjustedPath = new ArrayList<>();
        
        for (int i = 0; i < precomputed.getPath().size() - 1; i++) {
            GraphVertex current = precomputed.getPath().get(i);
            GraphVertex next = precomputed.getPath().get(i + 1);
            
            String edgeKey = current.getId() + "-" + next.getId();
            double currentWeight = trafficWeights.getOrDefault(edgeKey, current.getEdgeWeight(next.getId()));
            
            adjustedWeight += currentWeight;
            adjustedPath.add(current);
        }
        adjustedPath.add(precomputed.getPath().get(precomputed.getPath().size() - 1));
        
        return PathResult.builder()
            .path(adjustedPath)
            .totalWeight(adjustedWeight)
            .distance(precomputed.getDistance())
            .build();
    }
    
    /**
     * Find alternative routes for comparison
     */
    private List<AlternativeRoute> findAlternativeRoutes(GraphVertex start, GraphVertex end, 
                                                        Map<String, Double> trafficWeights) {
        
        // Use k-shortest paths algorithm to find alternatives
        List<PathResult> alternatives = graphService.findKShortestPaths(start, end, trafficWeights, 3);
        
        return alternatives.stream()
            .skip(1) // Skip the primary route
            .map(path -> AlternativeRoute.builder()
                .estimatedTimeMinutes(path.getTotalWeight())
                .distance(path.getDistance())
                .reason(determineRouteReason(path))
                .build())
            .toList();
    }
    
    /**
     * Calculate confidence score based on traffic data quality and historical accuracy
     */
    private double calculateConfidence(ETAPathResult pathResult) {
        double baseConfidence = 0.85;
        LocalDateTime now = LocalDateTime.now();
        
        // Reduce confidence during peak hours
        if (isPeakHour(now)) {
            baseConfidence -= 0.15;
        }
        
        // Increase confidence for shorter routes
        if (pathResult.getDistance() < 5.0) {
            baseConfidence += 0.1;
        }
        
        return Math.min(0.99, Math.max(0.5, baseConfidence));
    }
    
    /**
     * Fallback ETA calculation using simple distance/speed
     */
    private ETAResult fallbackETA(BigDecimal startLat, BigDecimal startLon, 
                                 BigDecimal endLat, BigDecimal endLon,
                                 LocalDateTime departureTime) {
        
        // Simple haversine distance calculation
        double distance = geoHashService.calculateDistance(
            geoHashService.generateGeoHash(startLat, startLon),
            geoHashService.generateGeoHash(endLat, endLon)
        );
        
        // Assume average city speed of 25 km/h
        double estimatedMinutes = (distance / 25.0) * 60.0;
        
        return ETAResult.builder()
            .estimatedTimeMinutes(estimatedMinutes)
            .estimatedArrival(departureTime.plusMinutes((long) estimatedMinutes))
            .distance(distance)
            .confidence(0.6) // Lower confidence for fallback
            .build();
    }
    
    private ETAPathResult combinePathSegments(List<PathSegment> segments) {
        List<GraphVertex> combinedPath = new ArrayList<>();
        double totalWeight = 0.0;
        double totalDistance = 0.0;
        
        for (PathSegment segment : segments) {
            combinedPath.addAll(segment.getPathResult().getPath());
            totalWeight += segment.getPathResult().getTotalWeight();
            totalDistance += segment.getPathResult().getDistance();
        }
        
        return ETAPathResult.builder()
            .path(combinedPath)
            .totalWeight(totalWeight)
            .distance(totalDistance)
            .build();
    }
    
    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
    }
    
    private String determineRouteReason(PathResult path) {
        if (path.getDistance() < 0.8) {
            return "Shorter distance";
        } else if (path.getTotalWeight() < 0.9) {
            return "Faster during current traffic";
        } else {
            return "Alternative highway route";
        }
    }
    
    // Data classes
    @lombok.Data
    @lombok.Builder
    public static class ETAResult {
        private double estimatedTimeMinutes;
        private LocalDateTime estimatedArrival;
        private List<GraphVertex> path;
        private double distance;
        private double confidence;
        private List<AlternativeRoute> alternativeRoutes;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ETAPathResult {
        private List<GraphVertex> path;
        private double totalWeight;
        private double distance;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AlternativeRoute {
        private double estimatedTimeMinutes;
        private double distance;
        private String reason;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PathSegment {
        private ETAPathResult pathResult;
        private String partition;
    }
}
