package com.p2p.realtime.repository;

import com.p2p.realtime.model.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, UUID> {

    /**
     * Find the most recent location for a given vehicle.
     */
    VehicleLocation findTopByVehicleIdOrderByTimestampDesc(UUID vehicleId);

    /**
     * Find locations for a vehicle within a time range, ordered by timestamp ascending.
     */
    List<VehicleLocation> findByVehicleIdAndTimestampBetweenOrderByTimestampAsc(
            UUID vehicleId, ZonedDateTime startTime, ZonedDateTime endTime);

    /**
     * Find recent locations by geohash prefix.
     * @deprecated Use findByGeohashesAndRecent instead for better performance with exact geohash matching.
     */
    @Deprecated
    @Query("SELECT v FROM VehicleLocation v WHERE v.geohash LIKE :geohashPrefix% AND v.timestamp > :cutoffTime ORDER BY v.timestamp DESC")
    List<VehicleLocation> findByGeohashPrefixAndRecent(
            @Param("geohashPrefix") String geohashPrefix,
            @Param("cutoffTime") ZonedDateTime cutoffTime);

    /**
     * Find recent locations by a list of geohashes.
     */
    @Query("SELECT v FROM VehicleLocation v WHERE v.geohash IN :geohashes AND v.timestamp > :cutoffTime ORDER BY v.timestamp DESC")
    List<VehicleLocation> findByGeohashesAndRecent(
            @Param("geohashes") List<String> geohashes,
            @Param("cutoffTime") ZonedDateTime cutoffTime);

    /**
     * Delete locations older than the specified cutoff time.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM VehicleLocation v WHERE v.timestamp < :cutoffTime")
    void deleteByTimestampBefore(@Param("cutoffTime") ZonedDateTime cutoffTime);
}