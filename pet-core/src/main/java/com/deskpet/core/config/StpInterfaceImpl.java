package com.deskpet.core.config;

import cn.dev33.satoken.stp.StpInterface;
import com.deskpet.core.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据加载实现
 * 实现 StpInterface 接口，告诉 Sa-Token 如何获取用户的权限和角色
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    /**
     * 返回用户拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return permissionService.getPermissionCodesByUserId(userId);
    }

    /**
     * 返回用户拥有的角色码集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return permissionService.getRoleCodesByUserId(userId);
    }
}
