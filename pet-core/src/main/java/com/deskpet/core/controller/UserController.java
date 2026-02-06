package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.*;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.LicenseCodeService;
import com.deskpet.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息、授权码、设备管理")
public class UserController {

    private final UserService userService;
    private final LicenseCodeService licenseCodeService;
    private final DeviceService deviceService;

    @GetMapping("/me")
    @SaCheckLogin
    @Operation(summary = "获取当前用户信息")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PutMapping("/me")
    @SaCheckLogin
    @Operation(summary = "更新当前用户信息")
    public UserResponse updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        return userService.updateCurrentUser(request);
    }

    @PostMapping("/me/licenses/activate")
    @SaCheckPermission("license:activate")
    @Operation(summary = "激活授权码", description = "使用授权码绑定设备")
    public LicenseCodeResponse activateLicense(@Valid @RequestBody ActivateLicenseRequest request) {
        return licenseCodeService.activate(request);
    }

    @GetMapping("/me/licenses")
    @SaCheckLogin
    @Operation(summary = "我的授权码列表")
    public List<LicenseCodeResponse> getMyLicenses() {
        return licenseCodeService.findByCurrentUser();
    }

    @GetMapping("/me/devices")
    @SaCheckLogin
    @Operation(summary = "我的设备列表", description = "获取当前用户通过授权码绑定的设备")
    public List<DeviceResponse> getMyDevices() {
        long userId = StpUtil.getLoginIdAsLong();
        List<String> deviceIds = licenseCodeService.findDeviceIdsByUserId(userId);
        return deviceService.findByIds(deviceIds);
    }
}
