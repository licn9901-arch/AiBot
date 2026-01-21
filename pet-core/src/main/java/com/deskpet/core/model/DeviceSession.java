package com.deskpet.core.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Entity
@Table(name = "device_session")
@Access(AccessType.FIELD)
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeviceSession {
    @Id
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "online", nullable = false)
    private boolean online;

    @Column(name = "gateway_instance_id")
    private String gatewayInstanceId;

    @Column(name = "ip")
    private String ip;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;
}
