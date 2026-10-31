package com.myhourly.jewellery.billing.repository;

import com.myhourly.jewellery.billing.entity.Invoice;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByOrderId(Long orderId);

    boolean existsByOrderIdAndStatusNot(Long orderId, InvoiceStatus status);
}