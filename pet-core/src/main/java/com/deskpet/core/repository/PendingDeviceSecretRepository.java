package com.deskpet.core.repository;

import com.deskpet.core.model.PendingDeviceSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PendingDeviceSecretRepository extends JpaRepository<PendingDeviceSecret, Long> {

    List<PendingDeviceSecret> findByBatchNo(String batchNo);

    void deleteByBatchNo(String batchNo);

    void deleteByCreatedAtBefore(Instant cutoff);
}
