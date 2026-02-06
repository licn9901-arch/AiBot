package com.deskpet.core.dto;

import com.deskpet.core.model.LicenseCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record LicenseCodeResponse(
    @Schema(description = "授权码ID", example = "1")
    Long id,
    @Schema(description = "授权码", example = "DKPT-A3B7-K9M2-X4N8")
    String code,
    @Schema(description = "批次号", example = "BATCH-2024-001")
    String batchNo,
    @Schema(description = "状态（UNUSED/ACTIVATED/REVOKED）", example = "ACTIVATED")
    String status,
    @Schema(description = "绑定设备ID", example = "pet001")
    String deviceId,
    @Schema(description = "产品标识", example = "deskpet-v1")
    String productKey,
    @Schema(description = "激活用户ID", example = "10001")
    Long userId,
    @Schema(description = "激活时间", example = "2026-01-21T11:00:00Z")
    Instant activatedAt,
    @Schema(description = "过期时间", example = "2026-12-31T23:59:59Z")
    Instant expiresAt,
    @Schema(description = "备注", example = "首发批次")
    String remark,
    @Schema(description = "创建时间", example = "2026-01-21T10:30:00Z")
    Instant createdAt
) {
    public static LicenseCodeResponse from(LicenseCode license) {
        return new LicenseCodeResponse(
            license.getId(),
            license.getCode(),
            license.getBatchNo(),
            license.getStatus().name(),
            license.getDeviceId(),
            license.getProductKey(),
            license.getUserId(),
            license.getActivatedAt(),
            license.getExpiresAt(),
            license.getRemark(),
            license.getCreatedAt()
        );
    }
}
