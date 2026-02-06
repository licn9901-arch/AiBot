package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record GenerateLicenseResponse(
    @Schema(description = "授权码ID", example = "1")
    Long id,
    @Schema(description = "授权码", example = "DKPT-A3B7-K9M2-X4N8")
    String code,
    @Schema(description = "绑定设备ID（已激活时）", example = "pet001")
    String deviceId,
    @Schema(description = "设备密钥（生成时返回）", example = "secret123")
    String deviceSecret,
    @Schema(description = "产品标识", example = "deskpet-v1")
    String productKey,
    @Schema(description = "批次号", example = "BATCH-2024-001")
    String batchNo,
    @Schema(description = "状态（UNUSED/ACTIVATED/REVOKED）", example = "UNUSED")
    String status,
    @Schema(description = "过期时间", example = "2026-12-31T23:59:59Z")
    Instant expiresAt,
    @Schema(description = "备注", example = "测试批次")
    String remark,
    @Schema(description = "创建时间", example = "2026-01-21T10:30:00Z")
    Instant createdAt
) {}
