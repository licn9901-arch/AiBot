package com.deskpet.core.repository;

import com.deskpet.core.model.LicenseCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseCodeRepository extends JpaRepository<LicenseCode, Long> {

    Optional<LicenseCode> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByDeviceIdAndStatus(String deviceId, LicenseCode.Status status);

    List<LicenseCode> findByUserId(Long userId);

    List<LicenseCode> findByUserIdAndStatus(Long userId, LicenseCode.Status status);

    List<LicenseCode> findByStatus(LicenseCode.Status status);

    List<LicenseCode> findByBatchNo(String batchNo);

    Page<LicenseCode> findByStatus(LicenseCode.Status status, Pageable pageable);

    Page<LicenseCode> findByBatchNo(String batchNo, Pageable pageable);

    Page<LicenseCode> findByStatusAndBatchNo(LicenseCode.Status status, String batchNo, Pageable pageable);

    @Query("SELECT lc FROM LicenseCode lc WHERE " +
           "(:status IS NULL OR lc.status = :status) AND " +
           "(:batchNo IS NULL OR lc.batchNo = :batchNo)")
    Page<LicenseCode> findByFilters(
        @Param("status") LicenseCode.Status status,
        @Param("batchNo") String batchNo,
        Pageable pageable
    );

    @Query("SELECT COUNT(lc) FROM LicenseCode lc WHERE lc.batchNo = :batchNo")
    long countByBatchNo(@Param("batchNo") String batchNo);

    @Query("SELECT COUNT(lc) FROM LicenseCode lc WHERE lc.batchNo = :batchNo AND lc.status = :status")
    long countByBatchNoAndStatus(@Param("batchNo") String batchNo, @Param("status") LicenseCode.Status status);

    @Query("SELECT lc.deviceId FROM LicenseCode lc WHERE lc.userId = :userId AND lc.status = :status")
    List<String> findDeviceIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") LicenseCode.Status status);

    default List<String> findDeviceIdsByUserId(Long userId) {
        return findDeviceIdsByUserIdAndStatus(userId, LicenseCode.Status.ACTIVATED);
    }
}
