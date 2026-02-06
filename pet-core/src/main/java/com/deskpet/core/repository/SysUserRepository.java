package com.deskpet.core.repository;

import com.deskpet.core.model.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<SysUser> findByStatus(String status);

    @Query("SELECT u FROM SysUser u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<SysUser> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT u FROM SysUser u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<SysUser> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM SysUser u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<SysUser> findByIdWithRolesAndPermissions(@Param("id") Long id);
}
