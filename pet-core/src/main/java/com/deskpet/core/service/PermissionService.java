package com.deskpet.core.service;

import com.deskpet.core.repository.SysRoleRepository;
import com.deskpet.core.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 权限服务 - 供 Sa-Token 使用
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;

    /**
     * 获取用户的权限码列表
     */
    public List<String> getPermissionCodesByUserId(Long userId) {
        return userRepository.findByIdWithRolesAndPermissions(userId)
            .map(user -> user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .toList())
            .orElse(Collections.emptyList());
    }

    /**
     * 获取用户的角色码列表
     */
    public List<String> getRoleCodesByUserId(Long userId) {
        return userRepository.findByIdWithRoles(userId)
            .map(user -> user.getRoles().stream()
                .map(role -> role.getCode())
                .toList())
            .orElse(Collections.emptyList());
    }
}
