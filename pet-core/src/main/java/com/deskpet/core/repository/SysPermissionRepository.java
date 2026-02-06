package com.deskpet.core.repository;

import com.deskpet.core.model.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {

    Optional<SysPermission> findByCode(String code);

    boolean existsByCode(String code);

    @Query(value = "SELECT DISTINCT p.code FROM sys_permission p " +
           "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
           "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
           "WHERE ur.user_id = :userId", nativeQuery = true)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT DISTINCT r.code FROM sys_role r " +
           "JOIN sys_user_role ur ON r.id = ur.role_id " +
           "WHERE ur.user_id = :userId", nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
