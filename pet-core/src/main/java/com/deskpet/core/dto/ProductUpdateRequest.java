package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
    @Schema(description = "产品名称", example = "桌宠V2升级版")
    @Size(max = 100, message = "产品名称长度不能超过100")
    String name,

    @Schema(description = "产品描述", example = "更新描述")
    @Size(max = 500, message = "描述长度不能超过500")
    String description,

    @Schema(description = "状态（ACTIVE/DEPRECATED）", example = "ACTIVE")
    String status
) {}
