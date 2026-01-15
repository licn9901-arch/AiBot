package com.deskpet.core.store;

import com.deskpet.core.model.DeviceSession;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private final ConcurrentHashMap<String, DeviceSession> sessions = new ConcurrentHashMap<>();

    public DeviceSession upsert(String deviceId, boolean online, String gatewayInstanceId, String ip) {
        DeviceSession session = new DeviceSession(deviceId, online, gatewayInstanceId, ip, Instant.now());
        sessions.put(deviceId, session);
        return session;
    }

    public Optional<DeviceSession> findById(String deviceId) {
        return Optional.ofNullable(sessions.get(deviceId));
    }
}
