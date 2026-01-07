package com.p2p.realtime.repository;

import com.p2p.realtime.model.TripTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripTrackingRepository extends JpaRepository<TripTracking, UUID> {

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    Optional<TripTracking> findByTripId(UUID tripId);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<TripTracking> findByUserId(UUID userId);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<TripTracking> findByVehicleIdAndStatus(UUID vehicleId, String status);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<TripTracking> findByUserIdAndStatus(UUID userId, String status);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    Optional<TripTracking> findByShareCode(String shareCode);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<TripTracking> findByDriverIdAndStatus(UUID driverId, String status);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<TripTracking> findByStatus(String status);
}