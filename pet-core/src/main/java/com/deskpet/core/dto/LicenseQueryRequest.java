package com.deskpet.core.dto;

import com.deskpet.core.model.LicenseCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record LicenseQueryRequest(
    @Schema(description = "授权码状态（UNUSED/ACTIVATED/REVOKED）", example = "UNUSED")
    LicenseCode.Status status,
    @Schema(description = "批次号", example = "BATCH-2024-001")
    String batchNo
) {}
