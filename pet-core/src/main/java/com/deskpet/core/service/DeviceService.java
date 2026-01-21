package com.deskpet.core.service;

import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.security.SecretHash;
import com.deskpet.core.security.SecretHasher;
import com.deskpet.core.repository.DeviceRepository;
import com.deskpet.core.repository.DeviceSessionRepository;
import com.deskpet.core.repository.TelemetryLatestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor()
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceSessionRepository sessionRepository;
    private final TelemetryLatestRepository telemetryLatestRepository;
    private final SecretHasher secretHasher;

    public Device register(String deviceId, String secret, String model, String remark) {
        if (deviceRepository.existsById(deviceId)) {
            throw new BusinessException(ErrorCode.DEVICE_ALREADY_EXISTS);
        }
        SecretHash hashed = secretHasher.hash(secret);
        Device device = new Device(deviceId, hashed.hash(), hashed.salt(), model, remark, Instant.now());
        return deviceRepository.save(device);
    }

    public Collection<Device> list() {
        return deviceRepository.findAll();
    }

    public Optional<Device> find(String deviceId) {
        return deviceRepository.findById(deviceId);
    }

    public Optional<DeviceSession> findSession(String deviceId) {
        return sessionRepository.findById(deviceId);
    }

    public Optional<TelemetryLatest> findTelemetry(String deviceId) {
        return telemetryLatestRepository.findById(deviceId);
    }

    public boolean verifySecret(Device device, String secret) {
        return secretHasher.matches(secret, device.secretSalt(), device.secretHash());
    }

    public DeviceSession markOnline(String deviceId, String gatewayInstanceId, String ip) {
        DeviceSession session = new DeviceSession(deviceId, true, gatewayInstanceId, ip, Instant.now());
        return sessionRepository.save(session);
    }

    public DeviceSession markOffline(String deviceId, String gatewayInstanceId, String ip) {
        DeviceSession session = new DeviceSession(deviceId, false, gatewayInstanceId, ip, Instant.now());
        return sessionRepository.save(session);
    }
}
