package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentInvoiceCancellationCheckTest {

    private static final Long INVOICE_ID = 70L;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentInvoiceCancellationCheck cancellationCheck;

    @Test
    void shouldBlockCancellationWhenInvoiceHasActivePayments() {
        when(paymentRepository.existsByInvoiceIdAndStatusIn(
                eq(INVOICE_ID), anyList())).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> cancellationCheck.assertCanCancel(invoice()));
    }

    @Test
    void shouldAllowCancellationWhenInvoiceHasNoActivePayments() {
        when(paymentRepository.existsByInvoiceIdAndStatusIn(
                eq(INVOICE_ID), anyList())).thenReturn(false);

        assertDoesNotThrow(
                () -> cancellationCheck.assertCanCancel(invoice()));
    }

    private InvoiceResponse invoice() {
        return new InvoiceResponse(INVOICE_ID, "INV-000070", 1L, 7L,
                InvoiceStatus.GENERATED, null, List.of(), null, null, null,
                null, new BigDecimal("1500.00"));
    }
}