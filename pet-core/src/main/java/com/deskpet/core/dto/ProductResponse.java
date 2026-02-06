package com.deskpet.core.dto;

import com.deskpet.core.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record ProductResponse(
    @Schema(description = "产品ID", example = "1")
    Long id,
    @Schema(description = "产品标识", example = "deskpet-v1")
    String productKey,
    @Schema(description = "产品名称", example = "桌宠V1")
    String name,
    @Schema(description = "产品描述", example = "默认产品")
    String description,
    @Schema(description = "状态（ACTIVE/DEPRECATED）", example = "ACTIVE")
    String status,
    @Schema(description = "创建时间", example = "2026-01-21T10:30:00Z")
    Instant createdAt,
    @Schema(description = "更新时间", example = "2026-01-22T10:30:00Z")
    Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getProductKey(),
            product.getName(),
            product.getDescription(),
            product.getStatus().name(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
