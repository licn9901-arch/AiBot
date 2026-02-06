package com.deskpet.core.repository;

import com.deskpet.core.model.ThingModelProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThingModelPropertyRepository extends JpaRepository<ThingModelProperty, Long> {

    List<ThingModelProperty> findByProductIdOrderBySortOrderAsc(Long productId);

    Optional<ThingModelProperty> findByProductIdAndIdentifier(Long productId, String identifier);

    boolean existsByProductIdAndIdentifier(Long productId, String identifier);

    void deleteByProductId(Long productId);
}
