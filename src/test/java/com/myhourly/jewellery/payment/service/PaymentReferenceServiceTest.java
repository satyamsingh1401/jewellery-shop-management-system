package com.myhourly.jewellery.payment.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReferenceServiceTest {

    private static final String STAFF = "staff1";

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPaymentShouldStoreTrimmedExternalReference() {
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
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                requestWithReference(null), STAFF);

        assertNull(response.getExternalReference());
    }

    private PaymentRequestDto requestWithReference(String reference) {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBillId(7L);
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setExternalReference(reference);
        return request;
    }
}