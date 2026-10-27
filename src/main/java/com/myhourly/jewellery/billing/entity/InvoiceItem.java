package com.myhourly.jewellery.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    private Long productId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal netWeight;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerGram;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal metalValue;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal makingCharge;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    protected InvoiceItem() {
    }

    public InvoiceItem(Long productId, BigDecimal netWeight, BigDecimal ratePerGram,
                       BigDecimal metalValue, BigDecimal makingCharge, BigDecimal lineTotal) {
        this.productId = productId;
        this.netWeight = netWeight;
        this.ratePerGram = ratePerGram;
        this.metalValue = metalValue;
        this.makingCharge = makingCharge;
        this.lineTotal = lineTotal;
    }

    void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public BigDecimal getNetWeight() { return netWeight; }
    public BigDecimal getRatePerGram() { return ratePerGram; }
    public BigDecimal getMetalValue() { return metalValue; }
    public BigDecimal getMakingCharge() { return makingCharge; }
    public BigDecimal getLineTotal() { return lineTotal; }
}