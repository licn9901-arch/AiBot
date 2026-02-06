package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeviceRegistrationRequest(
        @Schema(description = "设备ID", example = "pet001")
        @NotBlank String deviceId,
        @Schema(description = "设备密钥", example = "secret123")
        @NotBlank String secret,
        @Schema(description = "设备型号", example = "deskpet-v0.1")
        String model,
        @Schema(description = "产品标识", example = "deskpet-v1")
        String productKey,
        @Schema(description = "备注", example = "桌面测试设备")
        String remark
) {
}
