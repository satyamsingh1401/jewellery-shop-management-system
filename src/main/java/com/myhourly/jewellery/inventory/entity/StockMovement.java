package com.myhourly.jewellery.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit trail: one row for every change to stock. Rows are only ever inserted.
 * The unique key (product, type, referenceType, referenceId) is a second line of
 * defence against processing the same order twice (MySQL allows many NULLs, so
 * stock-in / adjustments without a reference are not affected).
 */
@Entity
@Table(name = "stock_movement",
        uniqueConstraints = @UniqueConstraint(name = "uk_movement_ref",
                columnNames = {"product_id", "movement_type", "reference_type", "reference_id"}),
        indexes = @Index(name = "idx_movement_product", columnList = "product_id, created_at"))
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    /** Signed: +5 means 5 units came in, -2 means 2 units went out. */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    /** Stock level right after this movement. */
    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reference_type", length = 30)
    private String referenceType;   // e.g. "ORDER"

    @Column(name = "reference_id", length = 64)
    private String referenceId;     // e.g. the order id

    @Column(length = 255)
    private String reason;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected StockMovement() { }

    public StockMovement(Long productId, MovementType movementType, int quantityChange, int balanceAfter,
                         String referenceType, String referenceId, String reason, String createdBy) {
        this.productId = productId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public MovementType getMovementType() { return movementType; }
    public int getQuantityChange() { return quantityChange; }
    public int getBalanceAfter() { return balanceAfter; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
