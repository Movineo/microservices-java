package com.p2p.realtime.repository;

import com.p2p.realtime.model.SafetyAlert;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafetyAlertRepository extends JpaRepository<SafetyAlert, UUID> {

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<SafetyAlert> findByActiveTrue();

    @Query("SELECT s FROM SafetyAlert s WHERE s.active = true AND (s.geohash LIKE :geohashPrefix% OR s.radiusKm >= :minRadius)")
    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<SafetyAlert> findActiveAlertsByGeohashPrefixOrRadius(
            @Param("geohashPrefix") String geohashPrefix,
            @Param("minRadius") Double minRadius);

    @Query("SELECT s FROM SafetyAlert s WHERE s.active = true AND s.geohash IN :geohashes")
    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<SafetyAlert> findActiveAlertsByGeohashes(@Param("geohashes") List<String> geohashes);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<SafetyAlert> findByReportedByAndActiveTrue(UUID reportedBy);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<SafetyAlert> findByAlertTypeAndActiveTrue(String alertType);
}