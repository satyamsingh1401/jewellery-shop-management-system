package com.myhourly.jewellery.payment.mapper;

import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponseDto toResponseDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponseDto dto = new PaymentResponseDto();

        dto.setId(payment.getId());
        dto.setInvoiceId(payment.getInvoiceId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setRefundedAmount(payment.getRefundedAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setCreatedBy(payment.getCreatedBy());
        dto.setExternalReference(payment.getExternalReference());
        dto.setRefundReason(payment.getRefundReason());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        return dto;
    }
}