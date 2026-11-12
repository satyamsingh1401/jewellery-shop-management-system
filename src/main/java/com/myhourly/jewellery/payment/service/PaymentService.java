package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.service.InvoiceService;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;
import com.myhourly.jewellery.payment.enums.OrderPaymentStatus;
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

    // Payments that already use up part of the invoice total (pending ones
    // are counted too, so two clients cannot pay the same rupee twice).
    private static final List<PaymentStatus> RESERVED_STATUSES = List.of(
            PaymentStatus.PENDING,
            PaymentStatus.COMPLETED,
            PaymentStatus.PARTIALLY_REFUNDED);

    // Payments whose money has really been received (minus refunds).
    private static final List<PaymentStatus> SUCCESSFUL_STATUSES = List.of(
            PaymentStatus.COMPLETED,
            PaymentStatus.PARTIALLY_REFUNDED);

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final OrderPaymentStatusUpdater orderPaymentStatusUpdater;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceService invoiceService,
                          OrderPaymentStatusUpdater orderPaymentStatusUpdater) {
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.orderPaymentStatusUpdater = orderPaymentStatusUpdater;
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

        validateOrderId(request.getOrderId());
        validateAmount(request.getAmount(), "Payment amount");

        if (request.getPaymentMethod() == null) {
            throw new BusinessValidationException(
                    "Payment method is required");
        }

        // Find the order's active invoice and lock it until this
        // transaction ends, so parallel payments cannot overpay it.
        InvoiceResponse invoice = invoiceService.getByIdForUpdate(
                invoiceService.getActiveByOrderId(request.getOrderId()).id());

        requireGenerated(invoice);

        BigDecimal outstanding = invoice.grandTotal().subtract(
                netAmount(invoice.id(), RESERVED_STATUSES));

        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new BusinessValidationException(
                    "Payment exceeds the outstanding invoice amount: "
                            + outstanding.max(BigDecimal.ZERO).toPlainString());
        }

        Payment payment = new Payment();
        payment.setInvoiceId(invoice.id());
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
    public List<PaymentResponseDto> getPaymentsForInvoice(Long invoiceId) {

        validateInvoiceId(invoiceId);

        return paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .map(PaymentMapper::toResponseDto)
                .toList();
    }

    // PENDING -> COMPLETED (payment confirmed by the configured process)
    @Transactional
    public PaymentResponseDto confirmPayment(Long id) {
        Payment payment = lockPending(id, PaymentStatus.COMPLETED);

        // Lock the invoice too and make sure it still accepts payments
        // (it may have been cancelled after this payment was created).
        InvoiceResponse invoice =
                invoiceService.getByIdForUpdate(payment.getInvoiceId());
        requireGenerated(invoice);

        transition(payment, PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        PaymentResponseDto response = saveAndMap(payment);

        // When the money received reaches the invoice total, Billing marks
        // the invoice PAID.
        if (netAmount(invoice.id(), SUCCESSFUL_STATUSES)
                .compareTo(invoice.grandTotal()) >= 0) {
            invoiceService.updateStatus(invoice.id(), InvoiceStatus.PAID);
        }

        syncOrderPaymentStatus(invoice);

        return response;
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

        // Lock the invoice too, so a refund and a new payment cannot race.
        InvoiceResponse invoice =
                invoiceService.getByIdForUpdate(payment.getInvoiceId());

        BigDecimal totalRefunded = refunded.add(refundAmount);

        payment.setRefundedAmount(totalRefunded);
        payment.setRefundReason(combinedReason);

        // Status change goes through the validated transition rules.
        PaymentStatus newStatus = totalRefunded.compareTo(paymentAmount) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;

        transition(payment, newStatus);

        PaymentResponseDto response = saveAndMap(payment);

        // If the refund drops the money received below the invoice total,
        // Billing moves the invoice from PAID back to GENERATED.
        if (invoice.status() == InvoiceStatus.PAID
                && netAmount(invoice.id(), SUCCESSFUL_STATUSES)
                        .compareTo(invoice.grandTotal()) < 0) {
            invoiceService.markUnpaid(invoice.id());
        }

        syncOrderPaymentStatus(invoice);

        return response;
    }

    // Tells the Order module how much of the order is paid. This runs in the
    // same transaction, so the invoice, the payments and the order never
    // disagree: if the Order module throws, the whole confirm/refund rolls back.
    private void syncOrderPaymentStatus(InvoiceResponse invoice) {
        BigDecimal received = netAmount(invoice.id(), SUCCESSFUL_STATUSES);

        OrderPaymentStatus status;
        if (received.compareTo(invoice.grandTotal()) >= 0) {
            status = OrderPaymentStatus.PAID;
        } else if (received.signum() > 0) {
            status = OrderPaymentStatus.PARTIALLY_PAID;
        } else {
            // Called only after a confirm or a refund: nothing left means
            // everything that was paid has been refunded.
            status = OrderPaymentStatus.REFUNDED;
        }

        orderPaymentStatusUpdater.updatePaymentStatus(
                invoice.orderId(), status);
    }

    // Saves an existing payment and flushes right away, so @PreUpdate has
    // already set updatedAt before the response DTO is built.
    private PaymentResponseDto saveAndMap(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        paymentRepository.flush();
        return PaymentMapper.toResponseDto(saved);
    }

    private Payment changePendingStatus(Long id, PaymentStatus target) {
        Payment payment = lockPending(id, target);
        transition(payment, target);
        return payment;
    }

    // Loads the payment with a row lock and checks that it is still PENDING.
    private Payment lockPending(Long id, PaymentStatus target) {

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

        return payment;
    }

    // Money counted for an invoice: payment amounts minus refunds,
    // for the given payment statuses.
    private BigDecimal netAmount(Long invoiceId,
                                 List<PaymentStatus> statuses) {
        BigDecimal paid = paymentRepository
                .sumAmountsByInvoiceIdAndStatusIn(invoiceId, statuses);
        BigDecimal refunded = paymentRepository
                .sumRefundedAmountsByInvoiceIdAndStatusIn(invoiceId, statuses);
        return paid.subtract(refunded);
    }

    private void requireGenerated(InvoiceResponse invoice) {
        if (invoice.status() != InvoiceStatus.GENERATED) {
            throw new BusinessValidationException(
                    "Payments are accepted only for GENERATED invoices; "
                            + "invoice " + invoice.id() + " is "
                            + invoice.status());
        }
    }

    private void transition(Payment payment, PaymentStatus newStatus) {
        try {
            payment.transitionTo(newStatus);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BusinessValidationException(
                    ex.getMessage());
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessValidationException(
                    "Order ID must be greater than zero");
        }
    }

    private void validateInvoiceId(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new BusinessValidationException(
                    "Invoice ID must be greater than zero");
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