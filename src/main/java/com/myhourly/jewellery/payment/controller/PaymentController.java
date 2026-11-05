package com.myhourly.jewellery.payment.controller;

import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;
import com.myhourly.jewellery.payment.dto.RefundRequestDto;
import com.myhourly.jewellery.payment.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request,
            Principal principal) {

        // The recorder comes from the authenticated user, never from the
        // request body.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(
                        request, principal.getName()));
    }

    @GetMapping
    public List<PaymentResponseDto> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public PaymentResponseDto getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @GetMapping("/invoice/{invoiceId}")
    public List<PaymentResponseDto> getPaymentsForInvoice(
            @PathVariable Long invoiceId) {
        return paymentService.getPaymentsForInvoice(invoiceId);
    }

    @PostMapping("/{paymentId}/confirm")
    public PaymentResponseDto confirmPayment(@PathVariable Long paymentId) {
        return paymentService.confirmPayment(paymentId);
    }

    @PostMapping("/{paymentId}/fail")
    public PaymentResponseDto failPayment(@PathVariable Long paymentId) {
        return paymentService.failPayment(paymentId);
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentResponseDto cancelPayment(@PathVariable Long paymentId) {
        return paymentService.cancelPayment(paymentId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponseDto recordRefund(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequestDto request) {

        return paymentService.recordRefund(
                paymentId,
                request.getAmount(),
                request.getReason());
    }
}