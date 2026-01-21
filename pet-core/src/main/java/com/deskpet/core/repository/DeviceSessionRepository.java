package com.deskpet.core.repository;

import com.deskpet.core.model.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, String> {
}
