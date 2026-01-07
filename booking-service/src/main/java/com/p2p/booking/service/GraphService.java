package com.p2p.booking.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Graph service implementing the partitioned road network for ETA calculations
 * Based on Uber's approach to graph partitioning and pre-computation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {
    
    private final Map<String, GraphPartition> partitions = new ConcurrentHashMap<>();
    // Simple in-memory cache for precomputed paths in production this would be Redis/database
    private final Map<String, PathResult> precomputedPathsCache = new ConcurrentHashMap<>();
    
    /**
     * Initialize precomputed paths when application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializePrecomputedPaths() {
        log.info("Initializing graph service with precomputed paths...");
        precomputeCommonPaths();
    }
    
    /**
     * Find the nearest graph vertex to given coordinates
     */
    public GraphVertex findNearestVertex(BigDecimal latitude, BigDecimal longitude) {
        String partitionId = determinePartition(latitude, longitude);
        GraphPartition partition = partitions.computeIfAbsent(partitionId, 
            id -> createPartition(id, latitude, longitude));
        
        return partition.findNearestVertex(latitude, longitude);
    }
    
    /**
     * Get pre-computed path between two vertices with actual implementation
     */
    @Cacheable(value = "precomputedPaths", key = "#pathKey")
    public PathResult getPrecomputedPath(String pathKey) {
        // Check in-memory cache first
        PathResult cached = precomputedPathsCache.get(pathKey);
        if (cached != null) {
            log.debug("Found precomputed path for key: {}", pathKey);
            return cached;
        }
        
        // Try to compute and cache common paths on-the-fly
        String[] parts = pathKey.split("-");
        if (parts.length == 2) {
            String startId = parts[0];
            String endId = parts[1];
            
            // Find vertices by ID across all partitions
            GraphVertex start = findVertexById(startId);
            GraphVertex end = findVertexById(endId);
            
            if (start != null && end != null && start.getPartition().equals(end.getPartition())) {
                // Compute path using basic traffic weights
                Map<String, Double> basicWeights = new HashMap<>();
                PathResult computed = dijkstraWithinPartition(start, end, basicWeights);
                
                // Cache the result for future use
                precomputedPathsCache.put(pathKey, computed);
                log.debug("Computed and cached new path for key: {}", pathKey);
                return computed;
            }
        }
        
        return null; // No precomputed path available
    }
    
    /**
     * Precompute common paths for better performance
     */
    public void precomputeCommonPaths() {
        log.info("Starting precomputation of common paths...");
        
        for (GraphPartition partition : partitions.values()) {
            // Use getVertices() method to access all vertices for comprehensive path computation
            Collection<GraphVertex> allVertices = partition.getVertices();
            log.debug("Processing partition with {} vertices", allVertices.size());
            
            // Precompute paths between all boundary vertices in the partition
            for (GraphVertex start : partition.getBoundaryVertices()) {
                for (GraphVertex end : partition.getBoundaryVertices()) {
                    if (!start.equals(end)) {
                        String pathKey = start.getId() + "-" + end.getId();
                        
                        if (!precomputedPathsCache.containsKey(pathKey)) {
                            PathResult path = dijkstraWithinPartition(start, end, new HashMap<>());
                            precomputedPathsCache.put(pathKey, path);
                        }
                    }
                }
            }
        }
        
        log.info("Precomputed {} paths", precomputedPathsCache.size());
    }
    
    /**
     * Find vertex by ID across all partitions
     */
    private GraphVertex findVertexById(String vertexId) {
        for (GraphPartition partition : partitions.values()) {
            GraphVertex vertex = partition.findVertexById(vertexId);
            if (vertex != null) {
                return vertex;
            }
        }
        return null;
    }
    
    /**
     * Modified Dijkstra's algorithm for within-partition routing
     */
    public PathResult dijkstraWithinPartition(GraphVertex start, GraphVertex end, 
                                            Map<String, Double> trafficWeights) {
        
        Map<String, Double> distances = new HashMap<>();
        Map<String, GraphVertex> previous = new HashMap<>();
        PriorityQueue<GraphVertex> queue = new PriorityQueue<>(
            Comparator.comparing(v -> distances.getOrDefault(v.getId(), Double.MAX_VALUE))
        );
        
        // Initialize
        distances.put(start.getId(), 0.0);
        queue.offer(start);
        
        while (!queue.isEmpty()) {
            GraphVertex current = queue.poll();
            
            if (current.getId().equals(end.getId())) {
                break; // Found destination
            }
            
            for (GraphEdge edge : current.getEdges()) {
                GraphVertex neighbor = edge.destination();
                String edgeKey = current.getId() + "-" + neighbor.getId();
                
                // Use traffic-adjusted weight
                double weight = trafficWeights.getOrDefault(edgeKey, edge.weight());
                double newDistance = distances.get(current.getId()) + weight;
                
                if (newDistance < distances.getOrDefault(neighbor.getId(), Double.MAX_VALUE)) {
                    distances.put(neighbor.getId(), newDistance);
                    previous.put(neighbor.getId(), current);
                    queue.offer(neighbor);
                }
            }
        }
        
        // Reconstruct path
        List<GraphVertex> path = reconstructPath(previous, start, end);
        double totalWeight = distances.getOrDefault(end.getId(), Double.MAX_VALUE);
        
        return new PathResult(path, totalWeight, calculateDistance(path));
    }
    
    /**
     * Find the best boundary vertex between partitions
     */
    public GraphVertex findBestBoundaryVertex(String fromPartition, String toPartition) {
        GraphPartition partition = partitions.get(fromPartition);
        if (partition == null) {
            return null;
        }
        
        // Find boundary vertices that connect to the target partition
        return partition.getBoundaryVertices().stream()
            .filter(vertex -> vertex.hasConnectionToPartition(toPartition))
            .min(Comparator.comparing(vertex -> vertex.getDistanceToPartition(toPartition)))
            .orElse(null);
    }
    
    /**
     * Find path between partitions using meta-graph
     */
    public List<String> findPartitionPath(String startPartition, String endPartition) {
        // Simple direct connection for now
        List<String> path = new ArrayList<>();
        path.add(startPartition);
        if (!startPartition.equals(endPartition)) {
            path.add(endPartition);
        }
        return path;
    }
    
    /**
     * Get pre-computed cross-partition path
     */
    public PathResult getCrossPartitionPath(String fromPartition, String toPartition) {
        // First try to get a cached cross-partition path
        String cacheKey = fromPartition + "->" + toPartition;
        PathResult cached = precomputedPathsCache.get(cacheKey);
        if (cached != null) {
            log.debug("Found cached cross-partition path from {} to {}", fromPartition, toPartition);
            return cached;
        }
        
        // If not cached, create and cache a new one
        PathResult newPath = createDefaultCrossPartitionPath(fromPartition, toPartition);
        precomputedPathsCache.put(cacheKey, newPath);
        return newPath;
    }
    
    /**
     * Find k shortest paths for alternative routes
     */
    public List<PathResult> findKShortestPaths(GraphVertex start, GraphVertex end, 
                                             Map<String, Double> trafficWeights, int k) {
        List<PathResult> results = new ArrayList<>();
        
        // Primary path
        PathResult primary = dijkstraWithinPartition(start, end, trafficWeights);
        results.add(primary);
        
        // Generate alternatives by modifying edge weights
        for (int i = 1; i < k && i < 3; i++) {
            Map<String, Double> modifiedWeights = new HashMap<>(trafficWeights);
            
            // Increase weights of primary path edges to force alternatives
            List<GraphVertex> primaryPath = primary.getPath();
            for (int j = 0; j < primaryPath.size() - 1; j++) {
                String edgeKey = primaryPath.get(j).getId() + "-" + primaryPath.get(j + 1).getId();
                modifiedWeights.put(edgeKey, modifiedWeights.getOrDefault(edgeKey, 1.0) * 2.0);
            }
            
            PathResult alternative = dijkstraWithinPartition(start, end, modifiedWeights);
            if (!alternative.getPath().isEmpty()) {
                results.add(alternative);
            }
        }
        
        return results;
    }
    
    /**
     * Determine which partition contains the given coordinates
     */
    private String determinePartition(BigDecimal latitude, BigDecimal longitude) {
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();
        
        int latGrid = (int) Math.floor(lat * 100) / 10; // ~1km grid
        int lonGrid = (int) Math.floor(lon * 100) / 10;
        
        return "partition_" + latGrid + "_" + lonGrid;
    }
    
    /**
     * Create a new partition with sample vertices
     */
    private GraphPartition createPartition(String partitionId, BigDecimal centerLat, BigDecimal centerLon) {
        GraphPartition partition = new GraphPartition(partitionId);
        
        // Create sample vertices (intersections) around the center
        for (int i = 0; i < 10; i++) {
            double latOffset = (Math.random() - 0.5) * 0.01; // ~1km radius
            double lonOffset = (Math.random() - 0.5) * 0.01;
            
            GraphVertex vertex = new GraphVertex(
                partitionId + "_vertex_" + i,
                centerLat.add(BigDecimal.valueOf(latOffset)),
                centerLon.add(BigDecimal.valueOf(lonOffset)),
                partitionId
            );
            
            partition.addVertex(vertex);
        }
        
        // Connect vertices with edges (roads)
        partition.connectVertices();
        
        return partition;
    }
    
    /**
     * Reconstruct path from Dijkstra's previous map
     */
    private List<GraphVertex> reconstructPath(Map<String, GraphVertex> previous, 
                                            GraphVertex start, GraphVertex end) {
        List<GraphVertex> path = new ArrayList<>();
        GraphVertex current = end;
        
        while (current != null) {
            path.add(0, current);
            current = previous.get(current.getId());
        }
        
        return path.isEmpty() || !path.get(0).equals(start) ? Collections.emptyList() : path;
    }
    
    /**
     * Calculate total distance of a path
     */
    private double calculateDistance(List<GraphVertex> path) {
        double totalDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            GraphVertex current = path.get(i);
            GraphVertex next = path.get(i + 1);
            totalDistance += current.distanceTo(next.getLatitude(), next.getLongitude());
        }
        return totalDistance;
    }
    
    /**
     * Create a default cross-partition path when pre-computed one doesn't exist
     */
    private PathResult createDefaultCrossPartitionPath(String fromPartition, String toPartition) {
        double estimatedWeight = 10.0; // 10 minutes default
        double estimatedDistance = 5.0; // 5 km default
        
        return new PathResult(Collections.emptyList(), estimatedWeight, estimatedDistance);
    }
}

/**
 * Represents a partition of the road network graph
 */
class GraphPartition {
    private final Map<String, GraphVertex> vertices = new HashMap<>();
    @Getter
    private final List<GraphVertex> boundaryVertices = new ArrayList<>();
    
    public GraphPartition(String id) {
        // Constructor parameter used for initialization but id field not needed
    }
    
    public void addVertex(GraphVertex vertex) {
        vertices.put(vertex.getId(), vertex);
        // Mark as boundary vertex (simplified logic)
        if (Math.random() < 0.3) { // 30% chance of being boundary
            boundaryVertices.add(vertex);
        }
    }
    
    public GraphVertex findNearestVertex(BigDecimal latitude, BigDecimal longitude) {
        return vertices.values().stream()
            .min(Comparator.comparing(v -> v.distanceTo(latitude, longitude)))
            .orElse(null);
    }

    /**
     * Find vertex by ID within this partition
     */
    public GraphVertex findVertexById(String vertexId) {
        return vertices.get(vertexId);
    }
    
    /**
     * Get all vertices in this partition
     */
    public Collection<GraphVertex> getVertices() {
        return vertices.values();
    }

    public void connectVertices() {
        List<GraphVertex> vertexList = new ArrayList<>(vertices.values());
        
        for (int i = 0; i < vertexList.size(); i++) {
            GraphVertex vertex = vertexList.get(i);
            
            // Connect to 2-3 nearest neighbors
            vertexList.stream()
                .filter(v -> !v.equals(vertex))
                .sorted(Comparator.comparing(v -> vertex.distanceTo(v.getLatitude(), v.getLongitude())))
                .limit(3)
                .forEach(neighbor -> {
                    double distance = vertex.distanceTo(neighbor.getLatitude(), neighbor.getLongitude());
                    double travelTime = distance * 2.0; // Assume 30 km/h average speed
                    vertex.addEdge(new GraphEdge(neighbor, travelTime));
                });
        }
    }
}
