package com.p2p.booking.repository;

import com.p2p.booking.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    
    List<Provider> findByIsActiveTrue();
    
    List<Provider> findByProviderTypeAndIsActiveTrue(Provider.ProviderType providerType);
    
    Optional<Provider> findByLicenseNumber(String licenseNumber);
    
    Optional<Provider> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT p FROM Provider p WHERE p.isActive = true AND " +
           "(:providerType IS NULL OR p.providerType = :providerType) " +
           "ORDER BY p.rating DESC")
    List<Provider> findActiveProvidersByType(@Param("providerType") Provider.ProviderType providerType);
    
    @Query("SELECT p FROM Provider p JOIN p.vehicles v WHERE v.isActive = true " +
           "AND p.isActive = true AND v.status = 'AVAILABLE' " +
           "AND (:providerType IS NULL OR p.providerType = :providerType)")
    List<Provider> findProvidersWithAvailableVehicles(@Param("providerType") Provider.ProviderType providerType);
    
    // GeoHash-based provider queries
    @Query("SELECT p FROM Provider p WHERE p.isActive = true " +
           "AND p.geoHash IS NOT NULL " +
           "AND p.geoHash LIKE CONCAT(:geoHashPrefix, '%')")
    List<Provider> findProvidersByGeoHashPrefix(@Param("geoHashPrefix") String geoHashPrefix);
    
    @Query("SELECT p FROM Provider p WHERE p.isActive = true " +
           "AND p.geoHash IN :geoHashes " +
           "AND (:providerType IS NULL OR p.providerType = :providerType)")
    List<Provider> findProvidersByGeoHashesAndType(@Param("geoHashes") List<String> geoHashes,
                                                  @Param("providerType") Provider.ProviderType providerType);
    
    List<Provider> findByGeoHashIn(List<String> geoHashes);
    
    @Query("SELECT p FROM Provider p WHERE p.isActive = true " +
           "AND p.geoHash IS NOT NULL " +
           "AND (:providerType IS NULL OR p.providerType = :providerType) " +
           "ORDER BY p.rating DESC")
    List<Provider> findActiveProvidersWithGeoHash(@Param("providerType") Provider.ProviderType providerType);
    
    // Find providers within service radius
    @Query("SELECT p FROM Provider p WHERE p.isActive = true " +
           "AND p.geoHash IN :geoHashes " +
           "AND p.serviceRadiusKm >= :requiredRadius")
    List<Provider> findProvidersWithServiceRadius(@Param("geoHashes") List<String> geoHashes,
                                                 @Param("requiredRadius") java.math.BigDecimal requiredRadius);
}