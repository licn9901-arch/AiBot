package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ActivateLicenseRequest(
    @Schema(description = "授权码", example = "DKPT-A3B7-K9M2-X4N8")
    @NotBlank(message = "授权码不能为空")
    String code
) {}
