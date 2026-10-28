package com.myhourly.jewellery.billing.entity;

import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 40)
    private String invoiceNumber;

    @Column(nullable = false)
    private Long customerId;

    private Long orderId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(String invoiceNumber, Long customerId, Long orderId, BigDecimal subtotal,
                   BigDecimal discountAmount, BigDecimal taxableAmount, BigDecimal taxAmount,
                   BigDecimal grandTotal) {
        this.invoiceNumber = invoiceNumber;
        this.customerId = customerId;
        this.orderId = orderId;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxableAmount = taxableAmount;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
        this.status = InvoiceStatus.GENERATED;
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void assignInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Long getCustomerId() { return customerId; }
    public Long getOrderId() { return orderId; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<InvoiceItem> getItems() { return items; }

        public void changeStatus(InvoiceStatus newStatus) {
        if (this.status != InvoiceStatus.GENERATED) {
            throw new IllegalArgumentException(
                    "Invoice is already " + this.status + " and cannot be changed");
        }
        if (newStatus == InvoiceStatus.GENERATED) {
            throw new IllegalArgumentException("Invoice is already GENERATED");
        }
        this.status = newStatus;
    }
}