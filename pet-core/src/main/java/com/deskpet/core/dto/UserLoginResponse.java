package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UserLoginResponse(
    @Schema(description = "登录令牌", example = "xxx-xxx-xxx")
    String token,
    @Schema(description = "用户ID", example = "1")
    Long userId,
    @Schema(description = "用户名", example = "testuser")
    String username,
    @Schema(description = "角色列表", example = "[\"USER\"]")
    List<String> roles
) {}
