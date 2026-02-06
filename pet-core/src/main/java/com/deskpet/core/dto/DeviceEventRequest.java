package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 设备事件上报请求（网关调用）
 */
public record DeviceEventRequest(
    @Schema(description = "事件标识", example = "collision")
    @NotBlank(message = "事件标识不能为空")
    String eventId,

    @Schema(description = "事件类型（info/alert/error）", example = "alert")
    @NotBlank(message = "事件类型不能为空")
    String eventType,

    @Schema(description = "事件发生时间戳（毫秒）", example = "1730000000000")
    Long timestamp,

    @Schema(description = "事件参数", example = "{\"direction\":\"front\",\"intensity\":75}")
    Map<String, Object> params
) {}
