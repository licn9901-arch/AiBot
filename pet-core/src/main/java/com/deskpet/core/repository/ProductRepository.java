package com.deskpet.core.repository;

import com.deskpet.core.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductKey(String productKey);

    boolean existsByProductKey(String productKey);

    List<Product> findByStatus(Product.Status status);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.properties WHERE p.id = :id")
    Optional<Product> findByIdWithProperties(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.services WHERE p.id = :id")
    Optional<Product> findByIdWithServices(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.events WHERE p.id = :id")
    Optional<Product> findByIdWithEvents(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.properties " +
           "LEFT JOIN FETCH p.services " +
           "LEFT JOIN FETCH p.events " +
           "WHERE p.productKey = :productKey")
    Optional<Product> findByProductKeyWithThingModel(@Param("productKey") String productKey);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.properties " +
           "LEFT JOIN FETCH p.services " +
           "LEFT JOIN FETCH p.events " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithThingModel(@Param("id") Long id);
}
