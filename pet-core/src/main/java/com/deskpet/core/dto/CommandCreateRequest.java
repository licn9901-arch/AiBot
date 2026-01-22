package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CommandCreateRequest(
        @Schema(description = "命令类型", example = "move")
        @NotBlank String type,
        @Schema(description = "命令参数", example = "{\"direction\":\"forward\",\"speed\":0.6,\"durationMs\":800}")
        Map<String, Object> payload
) {
}
