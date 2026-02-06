package com.deskpet.core.repository;

import com.deskpet.core.model.ThingModelService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThingModelServiceRepository extends JpaRepository<ThingModelService, Long> {

    List<ThingModelService> findByProductIdOrderBySortOrderAsc(Long productId);

    Optional<ThingModelService> findByProductIdAndIdentifier(Long productId, String identifier);

    boolean existsByProductIdAndIdentifier(Long productId, String identifier);

    void deleteByProductId(Long productId);
}
