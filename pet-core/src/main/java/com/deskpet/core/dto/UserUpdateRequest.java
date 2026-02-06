package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Schema(description = "邮箱", example = "new@example.com")
    @Size(max = 100, message = "邮箱长度不能超过100")
    String email,

    @Schema(description = "手机号", example = "13900000000")
    @Size(max = 20, message = "手机号长度不能超过20")
    String phone,

    @Schema(description = "头像URL", example = "https://example.com/avatar.png")
    @Size(max = 255, message = "头像URL长度不能超过255")
    String avatar
) {}
