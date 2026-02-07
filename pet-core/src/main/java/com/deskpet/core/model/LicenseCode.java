package com.deskpet.core.model;

import com.deskpet.core.persistence.SnowflakeIdentifierGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;

@Entity
@Table(name = "license_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseCode {

    public enum Status {
        UNUSED,
        ACTIVATED,
        REVOKED
    }

    @Id
    @GenericGenerator(name = "snowflake", type = SnowflakeIdentifierGenerator.class)
    @GeneratedValue(generator = "snowflake")
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.UNUSED;

    @Column(name = "device_id", length = 64, unique = true)
    private String deviceId;

    @Column(name = "product_key", length = 50)
    private String productKey;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean canActivate() {
        return status == Status.UNUSED && !isExpired();
    }
}
