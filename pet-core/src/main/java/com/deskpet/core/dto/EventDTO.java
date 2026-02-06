package com.deskpet.core.dto;

import com.deskpet.core.model.ThingModelEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record EventDTO(
    @Schema(description = "事件ID", example = "12")
    Long id,
    @Schema(description = "事件标识符", example = "collision")
    String identifier,
    @Schema(description = "事件名称", example = "碰撞事件")
    String name,
    @Schema(description = "事件类型（info/alert/error）", example = "alert")
    String eventType,
    @Schema(description = "输出参数列表", example = "[{\"identifier\":\"direction\",\"name\":\"方向\",\"dataType\":\"STRING\"},{\"identifier\":\"intensity\",\"name\":\"强度\",\"dataType\":\"INT\"}]")
    List<Map<String, Object>> outputParams,
    @Schema(description = "事件描述", example = "设备发生碰撞时触发")
    String description,
    @Schema(description = "排序值（越小越靠前）", example = "0")
    Integer sortOrder
) {
    public static EventDTO from(ThingModelEvent event) {
        return new EventDTO(
            event.getId(),
            event.getIdentifier(),
            event.getName(),
            event.getEventType(),
            event.getOutputParams(),
            event.getDescription(),
            event.getSortOrder()
        );
    }
}
