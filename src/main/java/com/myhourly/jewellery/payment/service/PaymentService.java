
package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.payment.Payment;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.mapper.PaymentMapper;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        Payment payment = new Payment();

        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRefundedAmount(BigDecimal.ZERO);

        Payment savedPayment = paymentRepository.save(payment);

        return PaymentMapper.toResponseDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + id));

        return PaymentMapper.toResponseDto(payment);
    }

    @Transactional
    public PaymentResponseDto recordRefund(
            Long id, BigDecimal refundAmount, String refundReason) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + id));

        if (payment.getStatus() != PaymentStatus.COMPLETED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only successful payments can be refunded");
        }

        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount must be greater than zero");
        }

        BigDecimal refunded = payment.getRefundedAmount() == null
                ? BigDecimal.ZERO : payment.getRefundedAmount();

        if (refunded.add(refundAmount).compareTo(payment.getAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund exceeds the refundable amount");
        }

        BigDecimal totalRefunded = refunded.add(refundAmount);
        payment.setRefundedAmount(totalRefunded);
        payment.setRefundReason(refundReason);

        payment.setStatus(
                totalRefunded.compareTo(payment.getAmount()) == 0
                        ? PaymentStatus.REFUNDED
                        : PaymentStatus.PARTIALLY_REFUNDED);

        return PaymentMapper.toResponseDto(paymentRepository.save(payment));
    }
}
