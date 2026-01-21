package com.deskpet.core.repository;

import com.deskpet.core.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {
}
