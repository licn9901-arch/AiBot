package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GenerateBatchResult(
    @Schema(description = "批次号", example = "BATCH-2024-001")
    String batchNo,
    @Schema(description = "生成的授权码列表", example = "[{\"id\":1,\"code\":\"DKPT-A3B7-K9M2-X4N8\",\"productKey\":\"deskpet-v1\",\"status\":\"UNUSED\"}]")
    List<GenerateLicenseResponse> items
) {}
