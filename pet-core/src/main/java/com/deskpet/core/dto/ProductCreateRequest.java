package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
    @Schema(description = "产品标识", example = "deskpet-v2")
    @NotBlank(message = "产品标识不能为空")
    @Size(max = 50, message = "产品标识长度不能超过50")
    String productKey,

    @Schema(description = "产品名称", example = "桌宠V2")
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100")
    String name,

    @Schema(description = "产品描述", example = "第二代桌宠设备")
    @Size(max = 500, message = "描述长度不能超过500")
    String description
) {}
