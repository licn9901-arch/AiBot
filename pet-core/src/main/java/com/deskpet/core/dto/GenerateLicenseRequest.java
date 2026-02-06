package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record GenerateLicenseRequest(
    @Schema(description = "生成数量", example = "10")
    @Min(value = 1, message = "生成数量至少为1")
    @Max(value = 1000, message = "单次生成数量不能超过1000")
    int count,

    @Schema(description = "产品标识", example = "deskpet-v1")
    @NotBlank(message = "产品标识不能为空")
    @Size(max = 50)
    String productKey,

    @Schema(description = "批次号", example = "BATCH-2024-001")
    @Size(max = 50, message = "批次号长度不能超过50")
    String batchNo,

    @Schema(description = "过期时间", example = "2026-12-31T23:59:59Z")
    Instant expiresAt,

    @Schema(description = "备注", example = "测试批次")
    @Size(max = 255, message = "备注长度不能超过255")
    String remark
) {}
