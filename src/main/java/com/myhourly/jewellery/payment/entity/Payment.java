package com.myhourly.jewellery.payment.entity;

import com.myhourly.jewellery.payment.enums.PaymentMethod;
import com.myhourly.jewellery.payment.enums.PaymentStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = @Index(
        name = "idx_payments_invoice_id", columnList = "invoice_id"))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The invoice this payment is for (Billing module). Kept as a plain id,
    // no foreign key, so the modules stay independent.
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01",
            message = "Payment amount must be at least 0.01")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @NotNull(message = "Refunded amount is required")
    @DecimalMin(value = "0.00",
            message = "Refunded amount cannot be negative")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3,
            message = "Currency must contain 3 characters")
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(length = 100)
    private String createdBy;

    // Reference from the payer's bank/UPI app (UPI transaction id, cheque
    // number, ...). Optional.
    @Column(length = 100)
    private String externalReference;

    @Column(length = 500)
    private String refundReason;

    private LocalDateTime paymentDate;

    @NotNull
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Payment() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        // paymentDate stays empty until the payment is confirmed.

        if (refundedAmount == null) {
            refundedAmount = BigDecimal.ZERO;
        }

        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PreRemove
    protected void preventDeletion() {
        throw new IllegalStateException(
                "Payment records cannot be deleted");
    }

    public void transitionTo(PaymentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Payment status is required");
        }

        if (status == newStatus) {
            return;
        }

        boolean allowed = switch (status) {
            case PENDING ->
                    newStatus == PaymentStatus.COMPLETED
                    || newStatus == PaymentStatus.FAILED
                    || newStatus == PaymentStatus.CANCELLED;

            case COMPLETED ->
                    newStatus == PaymentStatus.PARTIALLY_REFUNDED
                    || newStatus == PaymentStatus.REFUNDED;

            case PARTIALLY_REFUNDED ->
                    newStatus == PaymentStatus.REFUNDED;

            case FAILED, CANCELLED, REFUNDED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid payment status transition: "
                    + status + " -> " + newStatus);
        }

        this.status = newStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}