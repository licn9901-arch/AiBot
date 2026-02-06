package com.deskpet.core.repository;

import com.deskpet.core.model.TelemetryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryHistoryRepository extends JpaRepository<TelemetryHistory, Long> {
    java.util.List<TelemetryHistory> findByDeviceIdAndCreatedAtAfterOrderByCreatedAtAsc(String deviceId,
            java.time.Instant start);
}
