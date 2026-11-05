package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.service.InvoiceCancellationCheck;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import java.util.List;

import org.springframework.stereotype.Component;

// Billing calls this before it cancels an invoice. An invoice that still has
// money received (or a payment in progress) must not be cancelled.
// Uses the repository only, never PaymentService, so there is no circular
// dependency with Billing.
@Component
public class PaymentInvoiceCancellationCheck
        implements InvoiceCancellationCheck {

    private static final List<PaymentStatus> BLOCKING_STATUSES = List.of(
            PaymentStatus.PENDING,
            PaymentStatus.COMPLETED,
            PaymentStatus.PARTIALLY_REFUNDED);

    private final PaymentRepository paymentRepository;

    public PaymentInvoiceCancellationCheck(
            PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void assertCanCancel(InvoiceResponse invoice) {
        if (paymentRepository.existsByInvoiceIdAndStatusIn(
                invoice.id(), BLOCKING_STATUSES)) {
            throw new BusinessValidationException(
                    "Invoice " + invoice.id() + " has payments that are "
                            + "pending, completed or partly refunded; "
                            + "cancel or refund them first");
        }
    }
}