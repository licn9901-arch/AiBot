package com.deskpet.core.service;

import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.*;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.SysRole;
import com.deskpet.core.model.SysUser;
import com.deskpet.core.repository.SysRoleRepository;
import com.deskpet.core.repository.SysUserRepository;
import com.deskpet.core.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final OperationLogService operationLogService;
    private final PermissionService permissionService;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.email() != null && !request.email().isBlank()
            && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
        }

        // 检查手机号是否已存在
        if (request.phone() != null && !request.phone().isBlank()
            && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已被使用");
        }

        // 创建用户
        SysUser user = SysUser.builder()
            .username(request.username())
            .passwordHash(PasswordUtil.encode(request.password()))
            .email(request.email())
            .phone(request.phone())
            .status("ACTIVE")
            .build();

        // 分配默认角色 USER
        SysRole userRole = roleRepository.findByCode("USER")
            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "默认角色不存在"));
        user.getRoles().add(userRole);

        user = userRepository.save(user);

        log.info("User registered: username={}", user.getUsername());
        operationLogService.log(user.getId(), null, "REGISTER", Map.of("username", user.getUsername()));

        List<String> roles = permissionService.getRoleCodesByUserId(user.getId());
        return UserResponse.from(user, roles);
    }

    /**
     * 用户登录
     */
    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponse login(UserLoginRequest request, String ip, String userAgent) {
        // 查找用户
        SysUser user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误"));

        // 验证密码
        if (!PasswordUtil.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "用户名或密码错误");
        }

        // 检查用户状态
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 获取角色列表
        List<String> roles = permissionService.getRoleCodesByUserId(user.getId());

        log.info("User logged in: username={}, userId={}", user.getUsername(), user.getId());
        operationLogService.logSync(user.getId(), null, "LOGIN",
            Map.of("username", user.getUsername()), ip, userAgent);

        return new UserLoginResponse(token, user.getId(), user.getUsername(), roles);
    }

    /**
     * 退出登录
     */
    public void logout() {
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            operationLogService.log(userId, null, "LOGOUT", null);
            StpUtil.logout();
        }
    }

    /**
     * 获取当前用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        List<String> roles = permissionService.getRoleCodesByUserId(userId);
        return UserResponse.from(user, roles);
    }

    /**
     * 更新当前用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 检查邮箱是否被其他用户使用
        if (request.email() != null && !request.email().isBlank()
            && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
            }
            user.setEmail(request.email());
        }

        // 检查手机号是否被其他用户使用
        if (request.phone() != null && !request.phone().isBlank()
            && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已被使用");
            }
            user.setPhone(request.phone());
        }

        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }

        user = userRepository.save(user);
        operationLogService.log(userId, null, "UPDATE_PROFILE", null);

        List<String> roles = permissionService.getRoleCodesByUserId(userId);
        return UserResponse.from(user, roles);
    }

    /**
     * 获取用户列表（管理员）
     */
    @Transactional(rollbackFor = Exception.class)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(user -> {
                List<String> roles = permissionService.getRoleCodesByUserId(user.getId());
                return UserResponse.from(user, roles);
            });
    }

    /**
     * 更新用户状态（管理员）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, String status) {
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (!status.equals("ACTIVE") && !status.equals("DISABLED")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "无效的状态值");
        }

        user.setStatus(status);
        userRepository.save(user);

        // 如果禁用用户，强制下线
        if ("DISABLED".equals(status)) {
            StpUtil.kickout(userId);
        }

        long operatorId = StpUtil.getLoginIdAsLong();
        operationLogService.log(operatorId, null, "UPDATE_USER_STATUS",
            Map.of("targetUserId", userId, "status", status));
    }

    /**
     * 根据ID获取用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse getUserById(Long userId) {
        SysUser user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        List<String> roles = permissionService.getRoleCodesByUserId(userId);
        return UserResponse.from(user, roles);
    }
}
