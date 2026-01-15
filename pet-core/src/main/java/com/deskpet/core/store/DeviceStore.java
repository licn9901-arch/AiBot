package com.deskpet.core.store;

import com.deskpet.core.model.Device;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceStore {
    private final ConcurrentHashMap<String, Device> devices = new ConcurrentHashMap<>();

    public Device save(Device device) {
        devices.put(device.deviceId(), device);
        return device;
    }

    public Optional<Device> findById(String deviceId) {
        return Optional.ofNullable(devices.get(deviceId));
    }

    public Collection<Device> findAll() {
        return devices.values();
    }
}
