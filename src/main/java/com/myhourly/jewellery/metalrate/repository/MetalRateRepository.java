package com.myhourly.jewellery.metalrate.repository;

import com.myhourly.jewellery.metalrate.entity.MetalRate;
import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MetalRateRepository extends JpaRepository<MetalRate, Long> {

    Optional<MetalRate> findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            MetalType metalType, Purity purity, LocalDateTime at);

    boolean existsByMetalTypeAndPurityAndEffectiveFrom(
            MetalType metalType, Purity purity, LocalDateTime effectiveFrom);

    @Query("""
            select r from MetalRate r
            where (:metalType is null or r.metalType = :metalType)
              and (:purity is null or r.purity = :purity)
              and (:fromTime is null or r.effectiveFrom >= :fromTime)
              and (:toTime is null or r.effectiveFrom <= :toTime)
            """)
    Page<MetalRate> search(
            @Param("metalType") MetalType metalType,
            @Param("purity") Purity purity,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);
}
