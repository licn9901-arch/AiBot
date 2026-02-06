package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PropertyCreateRequest(
    @Schema(description = "属性标识符", example = "temperature")
    @NotBlank(message = "属性标识不能为空")
    @Size(max = 50, message = "属性标识长度不能超过50")
    String identifier,

    @Schema(description = "属性名称", example = "温度")
    @NotBlank(message = "属性名称不能为空")
    @Size(max = 100, message = "属性名称长度不能超过100")
    String name,

    @Schema(description = "数据类型（INT/FLOAT/BOOL/STRING/ENUM/STRUCT）", example = "FLOAT")
    @NotBlank(message = "数据类型不能为空")
    String dataType,

    @Schema(description = "数据规格", example = "{\"min\":-20,\"max\":60,\"unit\":\"C\"}")
    Map<String, Object> specs,

    @Schema(description = "访问模式（R/RW）", example = "R")
    String accessMode,

    @Schema(description = "是否必填", example = "false")
    Boolean required,

    @Schema(description = "属性描述", example = "设备温度")
    @Size(max = 500, message = "描述长度不能超过500")
    String description,

    @Schema(description = "排序值（越小越靠前）", example = "0")
    Integer sortOrder
) {}
