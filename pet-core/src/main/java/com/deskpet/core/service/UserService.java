package com.deskpet.core.service;

import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.ForgotPasswordRequest;
import com.deskpet.core.dto.ResetPasswordRequest;
import com.deskpet.core.dto.UserLoginRequest;
import com.deskpet.core.dto.UserLoginResponse;
import com.deskpet.core.dto.UserRegisterRequest;
import com.deskpet.core.dto.UserResponse;
import com.deskpet.core.dto.UserUpdateRequest;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.AuthToken;
import com.deskpet.core.model.AuthTokenType;
import com.deskpet.core.model.SysRole;
import com.deskpet.core.model.SysUser;
import com.deskpet.core.repository.SysRoleRepository;
import com.deskpet.core.repository.SysUserRepository;
import com.deskpet.core.util.CosUtil;
import com.deskpet.core.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_STATUS_ACTIVE = "ACTIVE";
    private static final String USER_STATUS_DISABLED = "DISABLED";
    private static final String USER_STATUS_PENDING_ACTIVATION = "PENDING_ACTIVATION";

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final OperationLogService operationLogService;
    private final PermissionService permissionService;
    private final AuthTokenService authTokenService;
    private final AuthMailService authMailService;
    private final CosUtil cosUtil;

    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterRequest request) {
        validateRegisterRequest(request);

        SysUser user = SysUser.builder()
            .username(request.username())
            .passwordHash(PasswordUtil.encode(request.password()))
            .email(request.email())
            .phone(request.phone())
            .status(USER_STATUS_PENDING_ACTIVATION)
            .build();

        SysRole userRole = roleRepository.findByCode("USER")
            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "默认角色不存在"));
        user.getRoles().add(userRole);

        user = userRepository.save(user);

        String rawToken = authTokenService.createActivationToken(user);
        authMailService.sendActivationEmail(
            user.getEmail(),
            user.getUsername(),
            authTokenService.buildActivationLink(rawToken)
        );

        log.info("User registered and activation email sent: username={}", user.getUsername());
        operationLogService.log(user.getId(), null, "REGISTER", Map.of("username", user.getUsername()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void activate(String rawToken) {
        AuthToken token = authTokenService.consumeValidToken(rawToken, AuthTokenType.ACTIVATION);
        SysUser user = token.getUser();
        user.setStatus(USER_STATUS_ACTIVE);
        userRepository.save(user);

        log.info("User activated: userId={}", user.getId());
        operationLogService.log(user.getId(), null, "ACTIVATE_ACCOUNT", Map.of("userId", user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
            .filter(user -> USER_STATUS_ACTIVE.equals(user.getStatus()))
            .ifPresent(user -> {
                String rawToken = authTokenService.createPasswordResetToken(user);
                authMailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getUsername(),
                    authTokenService.buildPasswordResetLink(rawToken)
                );
                operationLogService.log(user.getId(), null, "REQUEST_PASSWORD_RESET", Map.of("userId", user.getId()));
            });
    }

    @Transactional(readOnly = true)
    public void validatePasswordResetToken(String rawToken) {
        authTokenService.getValidToken(rawToken, AuthTokenType.PASSWORD_RESET);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        AuthToken token = authTokenService.consumeValidToken(request.token(), AuthTokenType.PASSWORD_RESET);
        SysUser user = token.getUser();

        user.setPasswordHash(PasswordUtil.encode(request.newPassword()));
        userRepository.save(user);
        authTokenService.invalidateUnusedTokens(user.getId(), AuthTokenType.PASSWORD_RESET);
        StpUtil.kickout(user.getId());

        log.info("Password reset completed: userId={}", user.getId());
        operationLogService.log(user.getId(), null, "RESET_PASSWORD", Map.of("userId", user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponse login(UserLoginRequest request, String ip, String userAgent) {
        SysUser user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误"));

        if (!PasswordUtil.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "用户名或密码错误");
        }

        if (USER_STATUS_PENDING_ACTIVATION.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVATED, "账号尚未激活，请先查收邮件完成激活");
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        List<String> roles = permissionService.getRoleCodesByUserId(user.getId());

        log.info("User logged in: username={}, userId={}", user.getUsername(), user.getId());
        operationLogService.logSync(user.getId(), null, "LOGIN",
            Map.of("username", user.getUsername()), ip, userAgent);

        return new UserLoginResponse(token, user.getId(), user.getUsername(), roles);
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            operationLogService.log(userId, null, "LOGOUT", null);
            StpUtil.logout();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        List<String> roles = permissionService.getRoleCodesByUserId(userId);
        return buildUserResponse(user, roles);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (request.email() != null && !request.email().isBlank()
            && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
            }
            user.setEmail(request.email());
        }

        if (request.phone() != null && !request.phone().isBlank()
            && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已被使用");
            }
            user.setPhone(request.phone());
        }

        if (request.avatar() != null) {
            user.setAvatar(normalizeAvatarValue(request.avatar()));
        }

        user = userRepository.save(user);
        operationLogService.log(userId, null, "UPDATE_PROFILE", null);

        List<String> roles = permissionService.getRoleCodesByUserId(userId);
        return buildUserResponse(user, roles);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(user -> {
                List<String> roles = permissionService.getRoleCodesByUserId(user.getId());
                return buildUserResponse(user, roles);
            });
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, String status) {
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (!USER_STATUS_ACTIVE.equals(status) && !USER_STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "无效的状态值");
        }

        user.setStatus(status);
        userRepository.save(user);

        if (USER_STATUS_DISABLED.equals(status)) {
            StpUtil.kickout(userId);
        }

        long operatorId = StpUtil.getLoginIdAsLong();
        operationLogService.log(operatorId, null, "UPDATE_USER_STATUS",
            Map.of("targetUserId", userId, "status", status));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        List<String> roles = permissionService.getRoleCodesByUserId(userId);
        return buildUserResponse(user, roles);
    }

    private UserResponse buildUserResponse(SysUser user, List<String> roles) {
        String avatarKey = normalizeAvatarValue(user.getAvatar());
        String avatarUrl = cosUtil.resolveObjectUrl(avatarKey);
        return UserResponse.from(user, roles, avatarUrl, avatarKey);
    }

    private String normalizeAvatarValue(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        String normalized = cosUtil.resolveObjectKey(avatar);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
        }
        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已被使用");
        }
    }
}
