package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
    @Schema(description = "用户名", example = "testuser")
    @NotBlank(message = "用户名不能为空")
    String username,

    @Schema(description = "密码", example = "password123")
    @NotBlank(message = "密码不能为空")
    String password
) {}
