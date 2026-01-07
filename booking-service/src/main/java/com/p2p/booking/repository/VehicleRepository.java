package com.p2p.booking.repository;

import com.p2p.booking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
    List<Vehicle> findByIsActiveTrueAndStatus(Vehicle.VehicleStatus status);
    
    List<Vehicle> findByProviderIdAndIsActiveTrue(Long providerId);
    
    Optional<Vehicle> findByPlateNumber(String plateNumber);
    
    @Query("SELECT v FROM Vehicle v WHERE v.isActive = true AND v.status = 'AVAILABLE' " +
           "AND v.provider.isActive = true AND v.capacity >= :minCapacity " +
           "AND (:providerId IS NULL OR v.provider.id = :providerId)")
    List<Vehicle> findAvailableVehicles(@Param("minCapacity") Integer minCapacity, 
                                       @Param("providerId") Long providerId);
    
    @Query("SELECT v FROM Vehicle v WHERE v.isActive = true AND v.status = 'AVAILABLE' " +
           "AND v.provider.isActive = true AND v.provider.providerType = :providerType " +
           "AND v.capacity >= :minCapacity")
    List<Vehicle> findAvailableVehiclesByProviderType(@Param("providerType") com.p2p.booking.entity.Provider.ProviderType providerType,
                                                     @Param("minCapacity") Integer minCapacity);
    
    // GeoHash-based queries for location-based vehicle search
    @Query("SELECT v FROM Vehicle v WHERE v.isActive = true AND v.status = 'AVAILABLE' " +
           "AND v.currentGeoHash IS NOT NULL " +
           "AND v.currentGeoHash LIKE CONCAT(:geoHashPrefix, '%') " +
           "AND v.capacity >= :minCapacity")
    List<Vehicle> findAvailableVehiclesByGeoHashPrefix(@Param("geoHashPrefix") String geoHashPrefix,
                                                      @Param("minCapacity") Integer minCapacity);
    
    @Query("SELECT v FROM Vehicle v WHERE v.isActive = true AND v.status = 'AVAILABLE' " +
           "AND v.currentGeoHash IN :geoHashes " +
           "AND v.capacity >= :minCapacity " +
           "AND (:providerType IS NULL OR v.provider.providerType = :providerType)")
    List<Vehicle> findAvailableVehiclesByGeoHashes(@Param("geoHashes") List<String> geoHashes,
                                                  @Param("minCapacity") Integer minCapacity,
                                                  @Param("providerType") com.p2p.booking.entity.Provider.ProviderType providerType);
    
    @Query("SELECT v FROM Vehicle v WHERE v.currentGeoHash IS NOT NULL " +
           "AND v.currentGeoHash LIKE CONCAT(:geoHashPrefix, '%') " +
           "AND v.status IN ('AVAILABLE', 'IN_TRANSIT')")
    List<Vehicle> findVehiclesInGeoHashArea(@Param("geoHashPrefix") String geoHashPrefix);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.provider.id = :providerId " +
           "AND v.isActive = true AND v.status = 'AVAILABLE'")
    Long countAvailableVehiclesByProvider(@Param("providerId") Long providerId);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.isActive = true " +
           "AND v.status = 'AVAILABLE' " +
           "AND v.currentGeoHash LIKE CONCAT(:geoHashPrefix, '%')")
    Long countAvailableVehiclesInArea(@Param("geoHashPrefix") String geoHashPrefix);
}