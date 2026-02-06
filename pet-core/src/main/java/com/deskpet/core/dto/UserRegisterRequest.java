package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
    @Schema(description = "用户名", example = "testuser")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50之间")
    String username,

    @Schema(description = "密码", example = "password123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在6-100之间")
    String password,

    @Schema(description = "邮箱", example = "test@example.com")
    @Size(max = 100, message = "邮箱长度不能超过100")
    String email,

    @Schema(description = "手机号", example = "13800000000")
    @Size(max = 20, message = "手机号长度不能超过20")
    String phone
) {}
