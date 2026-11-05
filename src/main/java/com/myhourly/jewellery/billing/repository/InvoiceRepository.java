package com.myhourly.jewellery.billing.repository;

import com.myhourly.jewellery.billing.entity.Invoice;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByOrderId(Long orderId);

    boolean existsByOrderIdAndStatusNot(Long orderId, InvoiceStatus status);

    Optional<Invoice> findByOrderIdAndStatusNot(Long orderId, InvoiceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);
}