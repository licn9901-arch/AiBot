package com.deskpet.core.repository;

import com.deskpet.core.model.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, String> {
    List<DeviceSession> findByGatewayInstanceIdAndOnlineTrue(String gatewayInstanceId);
}
