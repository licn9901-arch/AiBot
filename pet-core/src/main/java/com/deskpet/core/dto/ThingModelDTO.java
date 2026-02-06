package com.deskpet.core.dto;

import com.deskpet.core.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * 完整物模型 DTO（包含属性、服务、事件）
 */
public record ThingModelDTO(
    @Schema(description = "产品ID", example = "1")
    Long id,
    @Schema(description = "产品标识", example = "deskpet-v1")
    String productKey,
    @Schema(description = "产品名称", example = "桌宠V1")
    String productName,
    @Schema(description = "产品描述", example = "默认产品")
    String description,
    @Schema(description = "状态（ACTIVE/DEPRECATED）", example = "ACTIVE")
    String status,
    @Schema(description = "属性列表", example = "[{\"identifier\":\"battery\",\"name\":\"电量\",\"dataType\":\"INT\",\"accessMode\":\"R\",\"required\":true}]")
    List<PropertyDTO> properties,
    @Schema(description = "服务列表", example = "[{\"identifier\":\"move\",\"name\":\"移动控制\",\"callType\":\"ASYNC\"}]")
    List<ServiceDTO> services,
    @Schema(description = "事件列表", example = "[{\"identifier\":\"collision\",\"name\":\"碰撞事件\",\"eventType\":\"alert\"}]")
    List<EventDTO> events,
    @Schema(description = "创建时间", example = "2026-01-21T10:30:00Z")
    Instant createdAt,
    @Schema(description = "更新时间", example = "2026-01-22T10:30:00Z")
    Instant updatedAt
) {
    public static ThingModelDTO from(Product product) {
        return new ThingModelDTO(
            product.getId(),
            product.getProductKey(),
            product.getName(),
            product.getDescription(),
            product.getStatus().name(),
            product.getProperties().stream()
                .map(PropertyDTO::from)
                .sorted((a, b) -> a.sortOrder().compareTo(b.sortOrder()))
                .toList(),
            product.getServices().stream()
                .map(ServiceDTO::from)
                .sorted((a, b) -> a.sortOrder().compareTo(b.sortOrder()))
                .toList(),
            product.getEvents().stream()
                .map(EventDTO::from)
                .sorted((a, b) -> a.sortOrder().compareTo(b.sortOrder()))
                .toList(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
