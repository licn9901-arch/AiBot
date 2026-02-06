package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.deskpet.core.dto.UserResponse;
import com.deskpet.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器（管理员）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理（管理员）", description = "用户列表、状态管理")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @SaCheckPermission("user:list")
    @Operation(summary = "用户列表")
    public Page<UserResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @GetMapping("/{id}")
    @SaCheckPermission("user:list")
    @Operation(summary = "用户详情")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("user:disable")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户，禁用后强制下线")
    public void updateStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateUserStatus(id, status);
    }
}
