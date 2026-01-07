package com.p2p.booking.repository;

import com.p2p.booking.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    
    List<Route> findByIsActiveTrue();
    
    List<Route> findByProviderIdAndIsActiveTrue(Long providerId);
    
    Optional<Route> findByStartLocationAndEndLocationAndProviderIdAndIsActiveTrue(
        String startLocation, String endLocation, Long providerId);
    
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND " +
           "LOWER(r.startLocation) LIKE LOWER(CONCAT('%', :startLocation, '%')) AND " +
           "LOWER(r.endLocation) LIKE LOWER(CONCAT('%', :endLocation, '%'))")
    List<Route> findRoutesByLocations(@Param("startLocation") String startLocation, 
                                     @Param("endLocation") String endLocation);
    
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND " +
           "r.provider.isActive = true AND " +
           "LOWER(r.startLocation) LIKE LOWER(CONCAT('%', :startLocation, '%')) AND " +
           "LOWER(r.endLocation) LIKE LOWER(CONCAT('%', :endLocation, '%')) AND " +
           "(:providerType IS NULL OR r.provider.providerType = :providerType)")
    List<Route> findRoutesByLocationsAndProviderType(@Param("startLocation") String startLocation, 
                                                    @Param("endLocation") String endLocation,
                                                    @Param("providerType") com.p2p.booking.entity.Provider.ProviderType providerType);
    
    // GeoHash-based route queries for efficient spatial search
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.provider.isActive = true " +
           "AND (r.startGeoHash LIKE CONCAT(:startGeoHashPrefix, '%') " +
           "OR r.startGeoHash IN :startGeoHashes) " +
           "AND (r.endGeoHash LIKE CONCAT(:endGeoHashPrefix, '%') " +
           "OR r.endGeoHash IN :endGeoHashes)")
    List<Route> findRoutesByGeoHashAreas(@Param("startGeoHashPrefix") String startGeoHashPrefix,
                                        @Param("endGeoHashPrefix") String endGeoHashPrefix,
                                        @Param("startGeoHashes") List<String> startGeoHashes,
                                        @Param("endGeoHashes") List<String> endGeoHashes);
    
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.provider.isActive = true " +
           "AND r.startGeoHash IN :startGeoHashes " +
           "AND r.endGeoHash IN :endGeoHashes " +
           "AND (:providerType IS NULL OR r.provider.providerType = :providerType)")
    List<Route> findRoutesByStartAndEndGeoHashes(@Param("startGeoHashes") List<String> startGeoHashes,
                                               @Param("endGeoHashes") List<String> endGeoHashes,
                                               @Param("providerType") com.p2p.booking.entity.Provider.ProviderType providerType);
    
    // Find routes with waypoints that pass through specific areas
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.provider.isActive = true " +
           "AND (r.waypointGeoHashes IS NOT NULL AND r.waypointGeoHashes != '') " +
           "AND (r.startGeoHash LIKE CONCAT(:geoHashPrefix, '%') " +
           "OR r.endGeoHash LIKE CONCAT(:geoHashPrefix, '%') " +
           "OR r.waypointGeoHashes LIKE CONCAT('%', :geoHashPrefix, '%'))")
    List<Route> findRoutesPassingThroughArea(@Param("geoHashPrefix") String geoHashPrefix);
    
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.provider.isActive = true " +
           "AND r.baseFare BETWEEN :minFare AND :maxFare ORDER BY r.baseFare ASC")
    List<Route> findRoutesByFareRange(@Param("minFare") BigDecimal minFare, 
                                     @Param("maxFare") BigDecimal maxFare);
    
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND " +
           "r.distanceKm BETWEEN :minDistance AND :maxDistance")
    List<Route> findRoutesByDistanceRange(@Param("minDistance") BigDecimal minDistance, 
                                         @Param("maxDistance") BigDecimal maxDistance);
    
    @Query("SELECT DISTINCT r.startLocation FROM Route r WHERE r.isActive = true " +
           "ORDER BY r.startLocation")
    List<String> findAllStartLocations();
    
    @Query("SELECT DISTINCT r.endLocation FROM Route r WHERE r.isActive = true " +
           "ORDER BY r.endLocation")
    List<String> findAllEndLocations();
    
    // Find optimal routes considering distance, fare, and provider rating
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.provider.isActive = true " +
           "AND r.startGeoHash IN :startGeoHashes " +
           "AND r.endGeoHash IN :endGeoHashes " +
           "ORDER BY (r.distanceKm * 0.3 + r.baseFare * 0.0001 + " +
           "COALESCE((5.0 - r.provider.rating) * 0.2, 1.0)) ASC")
    List<Route> findOptimalRoutesByGeoHashes(@Param("startGeoHashes") List<String> startGeoHashes,
                                           @Param("endGeoHashes") List<String> endGeoHashes);
}