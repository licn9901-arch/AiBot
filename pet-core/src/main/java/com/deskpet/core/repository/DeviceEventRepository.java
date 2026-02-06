package com.deskpet.core.repository;

import com.deskpet.core.model.DeviceEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

    List<DeviceEvent> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Page<DeviceEvent> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    List<DeviceEvent> findByDeviceIdAndCreatedAtAfterOrderByCreatedAtDesc(String deviceId, Instant after);

    Page<DeviceEvent> findByDeviceIdAndEventIdOrderByCreatedAtDesc(String deviceId, String eventId, Pageable pageable);

    Page<DeviceEvent> findByDeviceIdAndEventTypeOrderByCreatedAtDesc(String deviceId, String eventType, Pageable pageable);

    @Query("SELECT e FROM DeviceEvent e WHERE e.deviceId = :deviceId " +
           "AND (:eventId IS NULL OR e.eventId = :eventId) " +
           "AND (:eventType IS NULL OR e.eventType = :eventType) " +
           "AND (:startTime IS NULL OR e.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR e.createdAt <= :endTime) " +
           "ORDER BY e.createdAt DESC")
    Page<DeviceEvent> findByFilters(
            @Param("deviceId") String deviceId,
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM DeviceEvent e WHERE e.deviceId = :deviceId AND e.eventType = :eventType")
    long countByDeviceIdAndEventType(@Param("deviceId") String deviceId, @Param("eventType") String eventType);

    @Query("SELECT e.eventId, COUNT(e) FROM DeviceEvent e WHERE e.deviceId = :deviceId " +
           "AND e.createdAt >= :since GROUP BY e.eventId ORDER BY COUNT(e) DESC")
    List<Object[]> countByDeviceIdGroupByEventId(@Param("deviceId") String deviceId, @Param("since") Instant since);
}
