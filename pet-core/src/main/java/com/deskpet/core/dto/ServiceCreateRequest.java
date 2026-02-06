package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ServiceCreateRequest(
    @Schema(description = "服务标识符（对应指令 type）", example = "customAction")
    @NotBlank(message = "服务标识不能为空")
    @Size(max = 50, message = "服务标识长度不能超过50")
    String identifier,

    @Schema(description = "服务名称", example = "自定义动作")
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称长度不能超过100")
    String name,

    @Schema(description = "调用类型（ASYNC/SYNC）", example = "ASYNC")
    String callType,

    @Schema(description = "输入参数列表", example = "[{\"identifier\":\"actionId\",\"name\":\"动作ID\",\"dataType\":\"STRING\"}]")
    List<Map<String, Object>> inputParams,

    @Schema(description = "输出参数列表", example = "[]")
    List<Map<String, Object>> outputParams,

    @Schema(description = "服务描述", example = "执行自定义动作")
    @Size(max = 500, message = "描述长度不能超过500")
    String description,

    @Schema(description = "排序值（越小越靠前）", example = "0")
    Integer sortOrder
) {}
