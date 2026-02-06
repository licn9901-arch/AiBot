package com.deskpet.core.dto;

import com.deskpet.core.model.ThingModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record PropertyDTO(
    @Schema(description = "属性ID", example = "101")
    Long id,
    @Schema(description = "属性标识符", example = "battery")
    String identifier,
    @Schema(description = "属性名称", example = "电量")
    String name,
    @Schema(description = "数据类型（INT/FLOAT/BOOL/STRING/ENUM/STRUCT）", example = "INT")
    String dataType,
    @Schema(description = "数据规格", example = "{\"min\":0,\"max\":100,\"unit\":\"%\"}")
    Map<String, Object> specs,
    @Schema(description = "访问模式（R/RW）", example = "R")
    String accessMode,
    @Schema(description = "是否必填", example = "true")
    Boolean required,
    @Schema(description = "属性描述", example = "电量百分比")
    String description,
    @Schema(description = "排序值（越小越靠前）", example = "0")
    Integer sortOrder
) {
    public static PropertyDTO from(ThingModelProperty property) {
        return new PropertyDTO(
            property.getId(),
            property.getIdentifier(),
            property.getName(),
            property.getDataType(),
            property.getSpecs(),
            property.getAccessMode(),
            property.getRequired(),
            property.getDescription(),
            property.getSortOrder()
        );
    }
}
