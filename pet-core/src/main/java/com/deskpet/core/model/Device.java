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
@Table(name = "device")
@Access(AccessType.FIELD)
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Device {
    @Id
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "secret_salt", nullable = false)
    private String secretSalt;

    @Column(name = "model")
    private String model;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
