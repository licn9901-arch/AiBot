package com.deskpet.core.service;

import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.store.DeviceStore;
import com.deskpet.core.store.SessionStore;
import com.deskpet.core.store.TelemetryStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Service
public class DeviceService {
    private final DeviceStore deviceStore;
    private final SessionStore sessionStore;
    private final TelemetryStore telemetryStore;

    public DeviceService(DeviceStore deviceStore, SessionStore sessionStore, TelemetryStore telemetryStore) {
        this.deviceStore = deviceStore;
        this.sessionStore = sessionStore;
        this.telemetryStore = telemetryStore;
    }

    public Device register(String deviceId, String secret, String model, String remark) {
        Device device = new Device(deviceId, secret, model, remark, Instant.now());
        return deviceStore.save(device);
    }

    public Collection<Device> list() {
        return deviceStore.findAll();
    }

    public Optional<Device> find(String deviceId) {
        return deviceStore.findById(deviceId);
    }

    public Optional<DeviceSession> findSession(String deviceId) {
        return sessionStore.findById(deviceId);
    }

    public Optional<TelemetryLatest> findTelemetry(String deviceId) {
        return telemetryStore.findByDeviceId(deviceId);
    }

    public DeviceSession markOnline(String deviceId, String gatewayInstanceId, String ip) {
        return sessionStore.upsert(deviceId, true, gatewayInstanceId, ip);
    }

    public DeviceSession markOffline(String deviceId, String gatewayInstanceId, String ip) {
        return sessionStore.upsert(deviceId, false, gatewayInstanceId, ip);
    }
}
