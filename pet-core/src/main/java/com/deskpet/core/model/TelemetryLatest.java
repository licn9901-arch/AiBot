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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "telemetry_latest")
@Access(AccessType.FIELD)
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TelemetryLatest {
    @Id
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "telemetry", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> telemetry;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
