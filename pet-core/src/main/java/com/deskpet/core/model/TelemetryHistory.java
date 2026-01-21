package com.deskpet.core.model;

import com.deskpet.core.persistence.JsonMapConverter;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "telemetry_history")
@Access(AccessType.FIELD)
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelemetryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "telemetry", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> telemetry;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TelemetryHistory(String deviceId, Map<String, Object> telemetry, Instant createdAt) {
        this.deviceId = deviceId;
        this.telemetry = telemetry;
        this.createdAt = createdAt;
    }
}
