package com.p2p.booking.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of pathfinding algorithms containing route information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathResult {
    private List<GraphVertex> path;
    private double totalWeight;  // Total travel time in minutes
    private double distance;     // Total distance in kilometers
}
