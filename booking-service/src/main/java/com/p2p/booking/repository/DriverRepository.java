package com.p2p.booking.repository;

import com.p2p.booking.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    List<Driver> findByIsActiveTrue();
    
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    
    Optional<Driver> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT d FROM Driver d WHERE d.isActive = true AND d.vehicle IS NOT NULL " +
           "AND d.vehicle.isActive = true AND d.vehicle.status = 'AVAILABLE'")
    List<Driver> findAvailableDrivers();
    
    @Query("SELECT d FROM Driver d WHERE d.isActive = true AND " +
           "d.vehicle.provider.providerType = :providerType AND " +
           "d.vehicle.isActive = true AND d.vehicle.status = 'AVAILABLE'")
    List<Driver> findAvailableDriversByProviderType(@Param("providerType") com.p2p.booking.entity.Provider.ProviderType providerType);
    
    @Query("SELECT AVG(d.rating) FROM Driver d WHERE d.isActive = true AND d.rating IS NOT NULL")
    Double getAverageDriverRating();
}