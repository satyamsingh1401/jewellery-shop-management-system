package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.service.InvoiceService;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.payment.enums.PaymentMethod;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReferenceServiceTest {

    private static final String STAFF = "staff1";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPaymentShouldStoreTrimmedExternalReference() {
        stubOpenInvoice();
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                requestWithReference("  UPI-123456  "), STAFF);

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        assertEquals("UPI-123456", captor.getValue().getExternalReference());
        assertEquals("UPI-123456", response.getExternalReference());
    }

    @Test
    void createPaymentShouldTreatBlankReferenceAsNoReference() {
        stubOpenInvoice();
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                requestWithReference("   "), STAFF);

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        assertNull(captor.getValue().getExternalReference());
        assertNull(response.getExternalReference());
    }

    @Test
    void createPaymentShouldAllowMissingReference() {
        stubOpenInvoice();
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                requestWithReference(null), STAFF);

        assertNull(response.getExternalReference());
    }

    // Order 7 has one GENERATED invoice (id 70) with nothing paid yet.
    private void stubOpenInvoice() {
        InvoiceResponse invoice = new InvoiceResponse(
                70L, "INV-000070", 1L, 7L, InvoiceStatus.GENERATED, null,
                List.of(), null, null, null, null, new BigDecimal("5000.00"));

        when(invoiceService.getActiveByOrderId(7L)).thenReturn(invoice);
        when(invoiceService.getByIdForUpdate(70L)).thenReturn(invoice);
        when(paymentRepository.sumAmountsByInvoiceIdAndStatusIn(
                eq(70L), anyList())).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumRefundedAmountsByInvoiceIdAndStatusIn(
                eq(70L), anyList())).thenReturn(BigDecimal.ZERO);
    }

    private PaymentRequestDto requestWithReference(String reference) {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(7L);
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setExternalReference(reference);
        return request;
    }
}