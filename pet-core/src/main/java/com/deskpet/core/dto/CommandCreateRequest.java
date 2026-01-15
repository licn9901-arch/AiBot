package com.deskpet.core.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CommandCreateRequest(
        @NotBlank String type,
        Map<String, Object> payload
) {
}
