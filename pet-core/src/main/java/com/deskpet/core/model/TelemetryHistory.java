package com.deskpet.core.model;

import com.deskpet.core.persistence.SnowflakeIdentifierGenerator;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GenericGenerator(name = "snowflake", type = SnowflakeIdentifierGenerator.class)
    @GeneratedValue(generator = "snowflake")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @JdbcTypeCode(SqlTypes.JSON)
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
