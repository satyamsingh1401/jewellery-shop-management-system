package com.myhourly.jewellery.inventory.repository;

import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDescIdDesc(Long productId, Pageable pageable);

    boolean existsByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
            Long productId, MovementType movementType, String referenceType, String referenceId);

    Optional<StockMovement> findByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
            Long productId, MovementType movementType, String referenceType, String referenceId);
}
