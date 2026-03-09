package com.deskpet.core.controller;

import com.deskpet.core.dto.ForgotPasswordRequest;
import com.deskpet.core.dto.ResetPasswordRequest;
import com.deskpet.core.dto.TokenValidationResponse;
import com.deskpet.core.dto.UserLoginRequest;
import com.deskpet.core.dto.UserLoginResponse;
import com.deskpet.core.dto.UserRegisterRequest;
import com.deskpet.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、激活、登录、重置密码")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户并发送激活邮件")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activate")
    @Operation(summary = "激活账号")
    public ResponseEntity<Void> activate(@RequestParam String token) {
        userService.activate(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "申请重置密码")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reset-password/validate")
    @Operation(summary = "校验重置密码 token")
    public TokenValidationResponse validateResetPasswordToken(@RequestParam String token) {
        userService.validatePasswordResetToken(token);
        return new TokenValidationResponse(true);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "提交新密码")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功返回 Token")
    public UserLoginResponse login(@Valid @RequestBody UserLoginRequest request,
                                   HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return userService.login(request, ip, userAgent);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public void logout() {
        userService.logout();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
