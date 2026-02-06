package com.deskpet.core.dto;

import com.deskpet.core.model.ThingModelService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record ServiceDTO(
    @Schema(description = "服务ID", example = "201")
    Long id,
    @Schema(description = "服务标识符（对应指令 type）", example = "move")
    String identifier,
    @Schema(description = "服务名称", example = "移动控制")
    String name,
    @Schema(description = "调用类型（ASYNC/SYNC）", example = "ASYNC")
    String callType,
    @Schema(description = "输入参数列表", example = "[{\"identifier\":\"direction\",\"name\":\"方向\",\"dataType\":\"ENUM\"},{\"identifier\":\"speed\",\"name\":\"速度\",\"dataType\":\"FLOAT\"}]")
    List<Map<String, Object>> inputParams,
    @Schema(description = "输出参数列表", example = "[]")
    List<Map<String, Object>> outputParams,
    @Schema(description = "服务描述", example = "控制设备移动")
    String description,
    @Schema(description = "排序值（越小越靠前）", example = "0")
    Integer sortOrder
) {
    public static ServiceDTO from(ThingModelService service) {
        return new ServiceDTO(
            service.getId(),
            service.getIdentifier(),
            service.getName(),
            service.getCallType(),
            service.getInputParams(),
            service.getOutputParams(),
            service.getDescription(),
            service.getSortOrder()
        );
    }
}
