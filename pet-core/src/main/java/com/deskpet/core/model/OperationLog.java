package com.deskpet.core.model;

import com.deskpet.core.persistence.SnowflakeIdentifierGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "operation_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLog {

    @Id
    @GenericGenerator(name = "snowflake", type = SnowflakeIdentifierGenerator.class)
    @GeneratedValue(generator = "snowflake")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(nullable = false, length = 50)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(length = 50)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
