package com.myhourly.jewellery.product.entity;

import com.myhourly.jewellery.product.enums.MakingChargeType;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="products",uniqueConstraints = {
        @UniqueConstraint(name = "unique_products_sku",columnNames ="sku")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 50)
    private String sku;

    @Column(nullable = false,length = 150)
    private String name;

    @Column(nullable = false,length = 80)
    private String category;

    @Column(nullable = false,length = 20)
    @Enumerated(EnumType.STRING)
    private MetalType metalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private Purity purity;

    @Column(nullable = false,precision = 12,scale = 3)
    private BigDecimal grossWeight;

    @Column(nullable = false,precision = 12,scale = 3)
    private BigDecimal stoneWeight;

    @Column(nullable = false,precision = 12,scale = 3)
    private BigDecimal netWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private MakingChargeType makingChargeType;

    @Column(nullable = false,precision =12,scale = 2)
    private BigDecimal makingChargeValue;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable=false,updatable = false)
    private Instant createdAt;


    @Column(nullable=false)
    private Instant updatedAt;

    @PrePersist
    protected  void onCreate(){
        Instant now= Instant.now();
        createdAt=now;
        updatedAt=now;
    }

    protected  void onUpdate(){
        updatedAt=Instant.now();
    }

public void updateDetails(String sku, String name, String category,
                          MetalType metalType, Purity purity, BigDecimal grossWeight
                          ,BigDecimal stoneWeight, MakingChargeType makingChargeType,BigDecimal makingChargeValue){
        this.sku=sku;
        this.name=name;
        this.category=category;
        this.metalType=metalType;
        this.purity=purity;
        this.grossWeight=grossWeight;
        this.stoneWeight=stoneWeight;
        this.netWeight=grossWeight.subtract(stoneWeight);

        this.makingChargeType=makingChargeType;
        this.makingChargeValue=makingChargeValue;

}

public void changeStatus(boolean active){

        this.active=active;
}

}
