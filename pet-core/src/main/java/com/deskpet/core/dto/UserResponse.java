package com.deskpet.core.dto;

import com.deskpet.core.model.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record UserResponse(
    @Schema(description = "用户ID", example = "1")
    Long id,
    @Schema(description = "用户名", example = "testuser")
    String username,
    @Schema(description = "邮箱", example = "test@example.com")
    String email,
    @Schema(description = "手机号", example = "13800000000")
    String phone,
    @Schema(description = "头像URL", example = "https://example.com/avatar.png")
    String avatar,
    @Schema(description = "状态", example = "ACTIVE")
    String status,
    @Schema(description = "角色列表", example = "[\"USER\"]")
    List<String> roles,
    @Schema(description = "创建时间", example = "2026-01-21T10:30:00Z")
    Instant createdAt
) {
    public static UserResponse from(SysUser user) {
        List<String> roleNames = user.getRoles().stream()
            .map(role -> role.getCode())
            .toList();
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatar(),
            user.getStatus(),
            roleNames,
            user.getCreatedAt()
        );
    }

    public static UserResponse from(SysUser user, List<String> roles) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatar(),
            user.getStatus(),
            roles,
            user.getCreatedAt()
        );
    }
}
