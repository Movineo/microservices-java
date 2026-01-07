package com.p2p.booking.repository;

import com.p2p.booking.entity.TripLeg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripLegRepository extends JpaRepository<TripLeg, Long> {
    
    List<TripLeg> findByTripIdOrderByLegOrderAsc(Long tripId);
    
    List<TripLeg> findByTripIdAndStatus(Long tripId, TripLeg.LegStatus status);
    
    List<TripLeg> findByRouteId(Long routeId);
    
    @Query("SELECT tl FROM TripLeg tl WHERE tl.trip.id = :tripId " +
           "ORDER BY tl.legOrder ASC")
    List<TripLeg> findTripLegsByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT tl FROM TripLeg tl WHERE tl.status = :status " +
           "ORDER BY tl.trip.createdAt DESC")
    List<TripLeg> findTripLegsByStatus(@Param("status") TripLeg.LegStatus status);
    
    @Query("SELECT COUNT(tl) FROM TripLeg tl WHERE tl.trip.id = :tripId")
    Long countTripLegsByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT COUNT(tl) FROM TripLeg tl WHERE tl.trip.id = :tripId AND tl.status = 'COMPLETED'")
    Long countCompletedTripLegsByTripId(@Param("tripId") Long tripId);
}