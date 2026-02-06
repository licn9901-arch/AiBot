package com.deskpet.core.repository;

import com.deskpet.core.model.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    Optional<SysRole> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT r FROM SysRole r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<SysRole> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT r FROM SysRole r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    Optional<SysRole> findByCodeWithPermissions(@Param("code") String code);

    @Query(value = "SELECT DISTINCT r.* FROM sys_role r " +
           "JOIN sys_user_role ur ON r.id = ur.role_id " +
           "WHERE ur.user_id = :userId", nativeQuery = true)
    List<SysRole> findByUserId(@Param("userId") Long userId);
}
