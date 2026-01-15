package com.deskpet.core.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegistrationRequest(
        @NotBlank String deviceId,
        @NotBlank String secret,
        String model,
        String remark
) {
}
