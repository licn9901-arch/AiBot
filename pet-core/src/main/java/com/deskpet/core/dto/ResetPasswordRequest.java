package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @Schema(description = "重置 token")
    @NotBlank(message = "token 不能为空")
    String token,

    @Schema(description = "新密码", example = "password123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在6-100之间")
    String newPassword
) {
}
