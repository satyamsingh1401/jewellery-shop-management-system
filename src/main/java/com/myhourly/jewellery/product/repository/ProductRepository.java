package com.myhourly.jewellery.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myhourly.jewellery.product.entity.Product;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL
               OR LOWER(p.category) = LOWER(:category))
          AND (:metalType IS NULL OR p.metalType = :metalType)
          AND (:purity IS NULL OR p.purity = :purity)
          AND (:active IS NULL OR p.active = :active)
        """)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("metalType") MetalType metalType,
            @Param("purity") Purity purity,
            @Param("active") Boolean active,
            Pageable pageable
    );

    Optional<Product> findBySku(String sku);
}