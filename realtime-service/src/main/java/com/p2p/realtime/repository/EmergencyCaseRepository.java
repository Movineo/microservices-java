package com.p2p.realtime.repository;

import com.p2p.realtime.model.EmergencyCase;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface EmergencyCaseRepository extends JpaRepository<EmergencyCase, UUID> {

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<EmergencyCase> findByUserIdAndResolvedFalse(UUID userId);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<EmergencyCase> findByTripIdAndResolvedFalse(UUID tripId);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<EmergencyCase> findByResolvedFalse();

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<EmergencyCase> findByGeohashInAndResolvedFalse(Set<String> geohashes);
}