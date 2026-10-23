package com.myhourly.jewellery.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Current stock level of ONE product (quantity-based model).
 * Only productId is stored - the Product module owns the product details.
 * NOTE: availableQuantity is changed only through the atomic queries in
 * InventoryRepository, never by get/set + save (that would allow lost updates).
 */
@Entity
@Table(name = "inventory",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product", columnNames = "product_id"))
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    /** When availableQuantity falls to this level or below, a low-stock event is raised. */
    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Inventory() { } // required by JPA

    public Inventory(Long productId, int availableQuantity, int reorderLevel) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
