package com.myhourly.jewellery.metalrate.entity;

import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "metal_rates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_metal_rate_metal_purity_effective",
                columnNames = {"metal_type", "purity", "effective_from"}),
        indexes = @Index(
                name = "idx_metal_rate_lookup",
                columnList = "metal_type, purity, effective_from"))
public class MetalRate {

    public static final String DEFAULT_CURRENCY = "INR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metal_type", nullable = false, length = 20)
    private MetalType metalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "purity", nullable = false, length = 20)
    private Purity purity;

    @Column(name = "rate_per_gram", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerGram;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = DEFAULT_CURRENCY;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (currency == null) {
            currency = DEFAULT_CURRENCY;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MetalType getMetalType() {
        return metalType;
    }

    public void setMetalType(MetalType metalType) {
        this.metalType = metalType;
    }

    public Purity getPurity() {
        return purity;
    }

    public void setPurity(Purity purity) {
        this.purity = purity;
    }

    public BigDecimal getRatePerGram() {
        return ratePerGram;
    }

    public void setRatePerGram(BigDecimal ratePerGram) {
        this.ratePerGram = ratePerGram;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
