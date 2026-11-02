package com.myhourly.jewellery.product.entity;

import com.myhourly.jewellery.product.enums.MakingChargeType;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "unique_products_sku", columnNames = "sku")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MetalType metalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purity purity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal grossWeight;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal stoneWeight;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal netWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MakingChargeType makingChargeType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal makingChargeValue;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDetails(
            String name,
            String category,
            MetalType metalType,
            Purity purity,
            BigDecimal grossWeight,
            BigDecimal stoneWeight,
            MakingChargeType makingChargeType,
            BigDecimal makingChargeValue) {

        this.name = name;
        this.category = category;
        this.metalType = metalType;
        this.purity = purity;
        this.grossWeight = grossWeight;
        this.stoneWeight = stoneWeight;

        // Automatically calculate net weight
        this.netWeight = grossWeight.subtract(stoneWeight);

        this.makingChargeType = makingChargeType;
        this.makingChargeValue = makingChargeValue;
    }

    public void changeStatus(boolean active) {

        this.active = active;
    }

    public void generateSku() {
        if (this.id == null || this.metalType == null) {
            throw new IllegalStateException(
                    "Product ID and metal type are required to generate SKU"
            );
        }

        this.sku = this.metalType.name() + "-" + String.format("%06d", this.id);
    }

    public static Product create() {

        return new Product();
    }

}
