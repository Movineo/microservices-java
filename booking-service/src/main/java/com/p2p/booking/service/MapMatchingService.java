package com.p2p.booking.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map matching service using Kalman Filter and Viterbi algorithm
 * to accurately match noisy GPS data to road network
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapMatchingService {

    private final GraphService graphService;
    private final Map<String, KalmanFilter> activeFilters = new ConcurrentHashMap<>();
    private final Map<String, List<GPSObservation>> gpsHistory = new ConcurrentHashMap<>();

    /**
     * Match GPS coordinates to the most likely road segment
     */
    public GraphVertex matchToRoad(BigDecimal latitude, BigDecimal longitude, String tripId) {
        GPSObservation observation = new GPSObservation(latitude, longitude, LocalDateTime.now());

        // Get or create Kalman filter for this trip
        KalmanFilter filter = activeFilters.computeIfAbsent(tripId,
            id -> new KalmanFilter(latitude.doubleValue(), longitude.doubleValue()));

        // Apply Kalman filtering to smooth GPS noise
        KalmanState filteredState = filter.update(observation);

        // Store GPS history for Viterbi algorithm
        List<GPSObservation> history = gpsHistory.computeIfAbsent(tripId, id -> new ArrayList<>());
        history.add(observation);

        // Keep only last 10 observations for performance
        if (history.size() > 10) {
            history.remove(0);
        }

        // Use Viterbi algorithm to find most likely road sequence
        return findMostLikelyRoadSegment(filteredState, history);
    }

    /**
     * Use Viterbi algorithm to find most likely sequence of road segments
     */
    private GraphVertex findMostLikelyRoadSegment(KalmanState filteredState, List<GPSObservation> history) {

        if (history.size() < 2) {
            // Not enough history, just find nearest vertex
            return graphService.findNearestVertex(
                BigDecimal.valueOf(filteredState.getLatitude()),
                BigDecimal.valueOf(filteredState.getLongitude())
            );
        }

        // Get candidate road segments for current position
        List<RoadCandidate> candidates = findCandidateRoads(filteredState);

        if (candidates.isEmpty()) {
            // Fallback to nearest vertex
            return graphService.findNearestVertex(
                BigDecimal.valueOf(filteredState.getLatitude()),
                BigDecimal.valueOf(filteredState.getLongitude())
            );
        }

        // Apply Viterbi algorithm to find most likely sequence
        return viterbiDecoding(candidates, history);
    }

    /**
     * Viterbi algorithm implementation for map matching
     */
    private GraphVertex viterbiDecoding(List<RoadCandidate> candidates, List<GPSObservation> history) {
        if (candidates.size() == 1) {
            return candidates.get(0).vertex();
        }

        // Initialize Viterbi matrices
        double[][] probability = new double[history.size()][candidates.size()];

        // Initialize first observation
        for (int j = 0; j < candidates.size(); j++) {
            RoadCandidate candidate = candidates.get(j);
            GPSObservation obs = history.get(0);

            probability[0][j] = calculateEmissionProbability(candidate, obs);
        }

        // Forward pass
        for (int t = 1; t < history.size(); t++) {
            for (int j = 0; j < candidates.size(); j++) {
                RoadCandidate currentCandidate = candidates.get(j);
                GPSObservation currentObs = history.get(t);

                double maxProb = Double.NEGATIVE_INFINITY;

                for (int k = 0; k < candidates.size(); k++) {
                    RoadCandidate prevCandidate = candidates.get(k);

                    double transitionProb = calculateTransitionProbability(prevCandidate, currentCandidate);
                    double emissionProb = calculateEmissionProbability(currentCandidate, currentObs);

                    double prob = probability[t-1][k] + Math.log(transitionProb) + Math.log(emissionProb);

                    if (prob > maxProb) {
                        maxProb = prob;
                    }
                }

                probability[t][j] = maxProb;
            }
        }

        // Find best final state
        int bestFinalState = 0;
        double bestFinalProb = probability[history.size()-1][0];

        for (int j = 1; j < candidates.size(); j++) {
            if (probability[history.size()-1][j] > bestFinalProb) {
                bestFinalProb = probability[history.size()-1][j];
                bestFinalState = j;
            }
        }

        return candidates.get(bestFinalState).vertex();
    }

    /**
     * Calculate emission probability (how likely GPS observation matches road segment)
     */
    private double calculateEmissionProbability(RoadCandidate candidate, GPSObservation observation) {
        double distance = candidate.getDistanceToObservation(observation);

        // Use Gaussian distribution with standard deviation of 10 meters
        double sigma = 10.0; // meters
        double probability = Math.exp(-0.5 * Math.pow(distance / sigma, 2));

        return Math.max(probability, 0.001); // Minimum probability
    }

    /**
     * Calculate transition probability (how likely to move from one road to another)
     */
    private double calculateTransitionProbability(RoadCandidate from, RoadCandidate to) {
        if (from.equals(to)) {
            return 0.8; // High probability of staying on same road
        }

        // Check if roads are connected
        if (from.vertex().getEdges().stream()
            .anyMatch(edge -> edge.destination().equals(to.vertex()))) {
            return 0.15; // Medium probability for connected roads
        }

        // Low probability for non-connected roads
        return 0.05;
    }

    /**
     * Find candidate road segments near the filtered GPS position
     */
    private List<RoadCandidate> findCandidateRoads(KalmanState state) {
        List<RoadCandidate> candidates = new ArrayList<>();

        // Search radius of 100 meters
        double searchRadius = 0.1; // km

        // Find nearby vertices
        GraphVertex nearest = graphService.findNearestVertex(
            BigDecimal.valueOf(state.getLatitude()),
            BigDecimal.valueOf(state.getLongitude())
        );

        if (nearest != null) {
            double distance = nearest.distanceTo(
                BigDecimal.valueOf(state.getLatitude()),
                BigDecimal.valueOf(state.getLongitude())
            ) * 1000; // Convert to meters

            candidates.add(new RoadCandidate(nearest, distance));

            // Add connected road segments as candidates
            for (GraphEdge edge : nearest.getEdges()) {
                GraphVertex neighbor = edge.destination();
                double neighborDistance = neighbor.distanceTo(
                    BigDecimal.valueOf(state.getLatitude()),
                    BigDecimal.valueOf(state.getLongitude())
                ) * 1000;

                if (neighborDistance <= searchRadius * 1000) {
                    candidates.add(new RoadCandidate(neighbor, neighborDistance));
                }
            }
        }

        return candidates;
    }

    /**
     * Clean up resources for completed trips
     */
    public void cleanupTrip(String tripId) {
        activeFilters.remove(tripId);
        gpsHistory.remove(tripId);
        log.debug("Cleaned up map matching resources for trip: {}", tripId);
    }

    // Data classes
    record GPSObservation(BigDecimal latitude, BigDecimal longitude, LocalDateTime timestamp) {

    }

    record RoadCandidate(GraphVertex vertex, double distanceMeters) {

        public double getDistanceToObservation(GPSObservation obs) {
                return vertex.distanceTo(obs.latitude(), obs.longitude()) * 1000; // Convert to meters
            }

        @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                RoadCandidate that = (RoadCandidate) o;
                return Objects.equals(vertex, that.vertex);
            }

        @Override
            public int hashCode() {
                return Objects.hash(vertex);
            }
        }
}

/**
 * Kalman Filter implementation for GPS smoothing
 */
class KalmanFilter {
    private final KalmanState state;

    public KalmanFilter(double initialLat, double initialLon) {
        this.state = new KalmanState(initialLat, initialLon, 0.0, 0.0, 1.0);
    }

    /**
     * Update filter with new GPS observation
     */
    public KalmanState update(MapMatchingService.GPSObservation observation) {
        // Predict step
        predict();

        // Update step
        updateWithMeasurement(observation);

        return state;
    }

    /**
     * Predict next state based on motion model
     */
    private void predict() {
        // Simple constant velocity model
        double dt = 1.0; // 1 second time step

        state.setLatitude(state.getLatitude() + state.getVelocityLat() * dt);
        state.setLongitude(state.getLongitude() + state.getVelocityLon() * dt);

        // Increase uncertainty due to process noise
        // Process noise (vehicle movement uncertainty)
        double processNoise = 0.1;
        state.setUncertainty(state.getUncertainty() + processNoise);
    }

    /**
     * Update state with GPS measurement
     */
    private void updateWithMeasurement(MapMatchingService.GPSObservation observation) {
        // Kalman gain calculation
        // GPS measurement noise in meters
        double measurementNoise = 10.0;
        double kalmanGain = state.getUncertainty() / (state.getUncertainty() + measurementNoise);

        // Update position estimates
        double measuredLat = observation.latitude().doubleValue();
        double measuredLon = observation.longitude().doubleValue();

        double newLat = state.getLatitude() + kalmanGain * (measuredLat - state.getLatitude());
        double newLon = state.getLongitude() + kalmanGain * (measuredLon - state.getLongitude());

        // Update velocity estimates (simple finite difference)
        state.setVelocityLat(newLat - state.getLatitude());
        state.setVelocityLon(newLon - state.getLongitude());

        state.setLatitude(newLat);
        state.setLongitude(newLon);

        // Update uncertainty
        state.setUncertainty((1 - kalmanGain) * state.getUncertainty());
    }
}

/**
 * Kalman filter state representation
 */
@Setter
@Getter
class KalmanState {
    // Getters and setters
    private double latitude;
    private double longitude;
    private double velocityLat;
    private double velocityLon;
    private double uncertainty;

    public KalmanState(double latitude, double longitude, double velocityLat, double velocityLon, double uncertainty) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocityLat = velocityLat;
        this.velocityLon = velocityLon;
        this.uncertainty = uncertainty;
    }

}
