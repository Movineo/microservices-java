package com.p2p.booking.service;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; /**
 * Represents a vertex (intersection/location) in the graph
 */
@Getter
public class GraphVertex {
    // Getters
    private final String id;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String partition;
    private final List<GraphEdge> edges;
    
    public GraphVertex(String id, BigDecimal latitude, BigDecimal longitude, String partition) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.partition = partition;
        this.edges = new ArrayList<>();
    }
    
    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }
    
    public double getEdgeWeight(String destinationId) {
        return edges.stream()
            .filter(edge -> edge.destination().getId().equals(destinationId))
            .findFirst()
            .map(GraphEdge::weight)
            .orElse(Double.MAX_VALUE);
    }
    
    public boolean hasConnectionToPartition(String partitionId) {
        return edges.stream()
            .anyMatch(edge -> edge.destination().getPartition().equals(partitionId));
    }
    
    public double getDistanceToPartition(String partitionId) {
        return edges.stream()
            .filter(edge -> edge.destination().getPartition().equals(partitionId))
            .mapToDouble(GraphEdge::weight)
            .min()
            .orElse(Double.MAX_VALUE);
    }
    
    public double distanceTo(BigDecimal lat, BigDecimal lon) {
        double earthRadius = 6371.0; // km
        
        double lat1 = this.latitude.doubleValue();
        double lon1 = this.longitude.doubleValue();
        double lat2 = lat.doubleValue();
        double lon2 = lon.doubleValue();
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        // Extract repeated calculations to eliminate duplication
        double sinDeltaLat = Math.sin(deltaLatRad / 2);
        double sinDeltaLon = Math.sin(deltaLonRad / 2);
        
        double a = sinDeltaLat * sinDeltaLat +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   sinDeltaLon * sinDeltaLon;
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphVertex that = (GraphVertex) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
