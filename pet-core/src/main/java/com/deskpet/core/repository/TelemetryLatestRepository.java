package com.deskpet.core.repository;

import com.deskpet.core.model.TelemetryLatest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryLatestRepository extends JpaRepository<TelemetryLatest, String> {
}
