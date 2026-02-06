package com.deskpet.core.repository;

import com.deskpet.core.model.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findByUserId(Long userId);

    List<OperationLog> findByDeviceId(String deviceId);

    List<OperationLog> findByAction(String action);

    Page<OperationLog> findByUserId(Long userId, Pageable pageable);

    Page<OperationLog> findByDeviceId(String deviceId, Pageable pageable);

    @Query("SELECT ol FROM OperationLog ol WHERE " +
           "(:userId IS NULL OR ol.userId = :userId) AND " +
           "(:deviceId IS NULL OR ol.deviceId = :deviceId) AND " +
           "(:action IS NULL OR ol.action = :action) AND " +
           "(:startTime IS NULL OR ol.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR ol.createdAt <= :endTime)")
    Page<OperationLog> findByFilters(
        @Param("userId") Long userId,
        @Param("deviceId") String deviceId,
        @Param("action") String action,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );
}
