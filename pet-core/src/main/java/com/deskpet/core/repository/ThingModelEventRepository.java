package com.deskpet.core.repository;

import com.deskpet.core.model.ThingModelEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThingModelEventRepository extends JpaRepository<ThingModelEvent, Long> {

    List<ThingModelEvent> findByProductIdOrderBySortOrderAsc(Long productId);

    Optional<ThingModelEvent> findByProductIdAndIdentifier(Long productId, String identifier);

    boolean existsByProductIdAndIdentifier(Long productId, String identifier);

    void deleteByProductId(Long productId);
}
