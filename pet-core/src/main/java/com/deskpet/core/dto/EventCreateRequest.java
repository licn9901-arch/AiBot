package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record EventCreateRequest(
    @Schema(description = "事件标识符", example = "collision")
    @NotBlank(message = "事件标识不能为空")
    @Size(max = 50, message = "事件标识长度不能超过50")
    String identifier,

    @Schema(description = "事件名称", example = "碰撞事件")
    @NotBlank(message = "事件名称不能为空")
    @Size(max = 100, message = "事件名称长度不能超过100")
    String name,

    @Schema(description = "事件类型（info/alert/error）", example = "alert")
    @NotBlank(message = "事件类型不能为空")
    String eventType,

    @Schema(description = "输出参数列表", example = "[{\"identifier\":\"direction\",\"name\":\"方向\",\"dataType\":\"STRING\"},{\"identifier\":\"intensity\",\"name\":\"强度\",\"dataType\":\"INT\"}]")
    List<Map<String, Object>> outputParams,

    @Schema(description = "事件描述", example = "设备发生碰撞时触发")
    @Size(max = 500, message = "描述长度不能超过500")
    String description,

    @Schema(description = "排序值（越小越靠前）", example = "0")
    Integer sortOrder
) {}
