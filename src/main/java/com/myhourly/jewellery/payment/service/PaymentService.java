package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.mapper.PaymentMapper;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final int MAX_REASON_LENGTH = 500;

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponseDto createPayment(
            PaymentRequestDto request,
            String createdBy) {

        if (request == null) {
            throw new BusinessValidationException(
                    "Payment request is required");
        }

        if (createdBy == null || createdBy.isBlank()) {
            throw new BusinessValidationException(
                    "The payment recorder must be identified");
        }

        validateBillId(request.getBillId());
        validateAmount(request.getAmount(), "Payment amount");

        if (request.getPaymentMethod() == null) {
            throw new BusinessValidationException(
                    "Payment method is required");
        }

        Payment payment = new Payment();
        payment.setBillId(request.getBillId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setCreatedBy(createdBy.trim());
        payment.setExternalReference(
                normalizeReference(request.getExternalReference()));

        // Keep payment pending until the payment process confirms success.
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

        validateId(id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + id));

        return PaymentMapper.toResponseDto(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsForBill(Long billId) {

        validateBillId(billId);

        return paymentRepository.findByBillIdOrderByCreatedAtDesc(billId)
                .stream()
                .map(PaymentMapper::toResponseDto)
                .toList();
    }

    // PENDING -> COMPLETED (payment confirmed by the configured process)
    @Transactional
    public PaymentResponseDto confirmPayment(Long id) {
        Payment payment = changePendingStatus(id, PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        return saveAndMap(payment);
    }

    // PENDING -> FAILED
    @Transactional
    public PaymentResponseDto failPayment(Long id) {
        Payment payment = changePendingStatus(id, PaymentStatus.FAILED);
        return saveAndMap(payment);
    }

    // PENDING -> CANCELLED
    @Transactional
    public PaymentResponseDto cancelPayment(Long id) {
        Payment payment = changePendingStatus(id, PaymentStatus.CANCELLED);
        return saveAndMap(payment);
    }

    @Transactional
    public PaymentResponseDto recordRefund(
            Long id,
            BigDecimal refundAmount,
            String refundReason) {

        validateId(id);

        // Lock the payment row to prevent concurrent refund updates.
        Payment payment = paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + id));

        if (payment.getStatus() != PaymentStatus.COMPLETED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessValidationException(
                    "Only successful payments can be refunded");
        }

        validateAmount(refundAmount, "Refund amount");

        if (refundReason == null || refundReason.isBlank()) {
            throw new BusinessValidationException(
                    "Refund reason is required");
        }

        BigDecimal paymentAmount = payment.getAmount();

        if (paymentAmount == null || paymentAmount.signum() <= 0) {
            throw new BusinessValidationException(
                    "Payment has an invalid amount");
        }

        BigDecimal refunded = payment.getRefundedAmount() == null
                ? BigDecimal.ZERO
                : payment.getRefundedAmount();

        BigDecimal remainingRefundable = paymentAmount.subtract(refunded);

        if (remainingRefundable.signum() <= 0
                || refundAmount.compareTo(remainingRefundable) > 0) {
            throw new BusinessValidationException(
                    "Refund exceeds the remaining refundable amount");
        }

        // Keep earlier refund reasons for audit instead of overwriting them.
        String newReason = refundReason.trim();
        String combinedReason = isBlank(payment.getRefundReason())
                ? newReason
                : payment.getRefundReason() + "; " + newReason;

        if (combinedReason.length() > MAX_REASON_LENGTH) {
            throw new BusinessValidationException(
                    "Refund reason history exceeds " + MAX_REASON_LENGTH
                            + " characters; use a shorter reason");
        }

        BigDecimal totalRefunded = refunded.add(refundAmount);

        payment.setRefundedAmount(totalRefunded);
        payment.setRefundReason(combinedReason);

        // Status change goes through the validated transition rules.
        PaymentStatus newStatus = totalRefunded.compareTo(paymentAmount) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;

        transition(payment, newStatus);

        return saveAndMap(payment);
    }

    // Saves an existing payment and flushes right away, so @PreUpdate has
    // already set updatedAt before the response DTO is built.
    private PaymentResponseDto saveAndMap(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        paymentRepository.flush();
        return PaymentMapper.toResponseDto(saved);
    }

    private Payment changePendingStatus(Long id, PaymentStatus target) {

        validateId(id);

        // Lock the row so confirm/fail/cancel cannot race each other.
        Payment payment = paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + id));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException(
                    "Only pending payments can be moved to " + target
                            + "; current status: " + payment.getStatus());
        }

        transition(payment, target);
        return payment;
    }

    private void transition(Payment payment, PaymentStatus newStatus) {
        try {
            payment.transitionTo(newStatus);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BusinessValidationException(
                    ex.getMessage());
        }
    }

    private void validateBillId(Long billId) {
        if (billId == null || billId <= 0) {
            throw new BusinessValidationException(
                    "Bill ID must be greater than zero");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessValidationException(
                    "Payment ID must be greater than zero");
        }
    }

    private void validateAmount(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            throw new BusinessValidationException(
                    label + " must be at least 0.01");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessValidationException(
                    label + " cannot have more than 2 decimal places");
        }
    }

    // Blank reference means "no reference".
    private String normalizeReference(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}