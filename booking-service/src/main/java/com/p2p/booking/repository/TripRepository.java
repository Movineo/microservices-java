package com.p2p.booking.repository;

import com.p2p.booking.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    Optional<Trip> findByBookingId(String bookingId);
    
    List<Trip> findByUserId(String userId);
    
    List<Trip> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<Trip> findByStatus(Trip.TripStatus status);
    
    List<Trip> findByVehicleIdAndStatus(Long vehicleId, Trip.TripStatus status);
    
    List<Trip> findByDriverIdAndStatus(Long driverId, Trip.TripStatus status);
    
    @Query("SELECT t FROM Trip t WHERE t.userId = :userId AND t.status = :status " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findByUserIdAndStatus(@Param("userId") String userId, 
                                    @Param("status") Trip.TripStatus status);
    
    @Query("SELECT t FROM Trip t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findTripsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.vehicle.provider.id = :providerId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findTripsByProviderAndDateRange(@Param("providerId") Long providerId,
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.userId = :userId AND t.status = 'COMPLETED'")
    Long countCompletedTripsByUser(@Param("userId") String userId);
    
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.id = :driverId AND t.status = 'COMPLETED'")
    Long countCompletedTripsByDriver(@Param("driverId") Long driverId);
    
    @Query("SELECT t FROM Trip t WHERE t.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS') " +
           "AND t.scheduledPickupTime < :cutoffTime")
    List<Trip> findOverdueTrips(@Param("cutoffTime") LocalDateTime cutoffTime);
}