package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenValidationResponse(
    @Schema(description = "token 是否有效", example = "true")
    boolean valid
) {
}
