package com.myhourly.jewellery.payment.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.payment.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Lock payment row during refund/update operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    // Retrieve all payments recorded against one invoice, newest first
    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    // True if the invoice has at least one payment in the given statuses
    boolean existsByInvoiceIdAndStatusIn(
            Long invoiceId, List<PaymentStatus> statuses);

    // Retrieve payments by status, newest first
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    // Retrieve payments in multiple statuses
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    // Calculate recorded amounts for a particular status
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = :status
        """)
    BigDecimal sumAmountsByStatus(
            @Param("status") PaymentStatus status);

    // Calculate refunds recorded against a particular status
    @Query("""
        SELECT COALESCE(SUM(p.refundedAmount), 0)
        FROM Payment p
        WHERE p.status = :status
        """)
    BigDecimal sumRefundedAmountsByStatus(
            @Param("status") PaymentStatus status);

    // Total recorded amount of one invoice's payments in the given statuses
    // (pass the "successful" statuses to exclude pending/failed/cancelled)
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.invoiceId = :invoiceId
          AND p.status IN :statuses
        """)
    BigDecimal sumAmountsByInvoiceIdAndStatusIn(
            @Param("invoiceId") Long invoiceId,
            @Param("statuses") List<PaymentStatus> statuses);

    // Total refunded amount of one invoice's payments in the given statuses
    @Query("""
        SELECT COALESCE(SUM(p.refundedAmount), 0)
        FROM Payment p
        WHERE p.invoiceId = :invoiceId
          AND p.status IN :statuses
        """)
    BigDecimal sumRefundedAmountsByInvoiceIdAndStatusIn(
            @Param("invoiceId") Long invoiceId,
            @Param("statuses") List<PaymentStatus> statuses);
}