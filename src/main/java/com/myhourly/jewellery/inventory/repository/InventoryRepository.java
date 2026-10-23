package com.myhourly.jewellery.inventory.repository;

import com.myhourly.jewellery.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    @Query("select i.availableQuantity from Inventory i where i.productId = :productId")
    Optional<Integer> findAvailableQuantity(@Param("productId") Long productId);

    @Query("select i from Inventory i where i.availableQuantity <= i.reorderLevel order by i.availableQuantity asc")
    List<Inventory> findLowStock();

    /**
     * THE anti-overselling query. Check and subtract happen in ONE SQL statement, and
     * MySQL locks the row while it runs, so two simultaneous requests for the last
     * item cannot both succeed. Returns 1 if stock was deducted, 0 if not enough stock
     * (or the product has no inventory row).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Inventory i set i.availableQuantity = i.availableQuantity - :qty, i.updatedAt = :now "
            + "where i.productId = :productId and i.availableQuantity >= :qty")
    int deductIfAvailable(@Param("productId") Long productId, @Param("qty") int qty, @Param("now") LocalDateTime now);

    /** Atomic add. Returns 1 if the product has an inventory row, otherwise 0. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Inventory i set i.availableQuantity = i.availableQuantity + :qty, i.updatedAt = :now "
            + "where i.productId = :productId")
    int addQuantity(@Param("productId") Long productId, @Param("qty") int qty, @Param("now") LocalDateTime now);
}
