package com.myhourly.jewellery.billing.service;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillCalculationResponse;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.dto.BillItemResponse;
import com.myhourly.jewellery.billing.dto.InvoiceCreateRequest;
import com.myhourly.jewellery.billing.dto.InvoiceItemResponse;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.entity.Invoice;
import com.myhourly.jewellery.billing.entity.InvoiceItem;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.repository.InvoiceRepository;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final String INVOICE_NUMBER_FORMAT = "INV-%06d";

    private final BillingCalculatorService calculator;
    private final InvoiceRepository repository;
    private final ObjectProvider<InvoiceCancellationCheck> cancellationChecks;

    public InvoiceService(BillingCalculatorService calculator, InvoiceRepository repository,
                          ObjectProvider<InvoiceCancellationCheck> cancellationChecks) {
        this.calculator = calculator;
        this.repository = repository;
        this.cancellationChecks = cancellationChecks;
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        // Order and customer verification will be added here once those modules are available.
        if (request.orderId() != null
                && repository.existsByOrderIdAndStatusNot(request.orderId(), InvoiceStatus.CANCELLED)) {
            throw new DuplicateResourceException(
                    "An active invoice already exists for order " + request.orderId());
        }

        BillCalculationResponse calc = calculator.calculate(new BillCalculationRequest(
                request.items(), request.discountType(), request.discountValue()));

        // The invoice number is derived from the database-generated id, so it is left
        // null here and assigned below, inside the same transaction.
        Invoice invoice = new Invoice(null, request.customerId(),
                request.orderId(), calc.subtotal(), calc.discount(), calc.taxableAmount(),
                calc.tax(), calc.grandTotal());

        for (int i = 0; i < request.items().size(); i++) {
            BillItemRequest in = request.items().get(i);
            BillItemResponse line = calc.items().get(i);
            invoice.addItem(new InvoiceItem(in.productId(), in.netWeight(), in.ratePerGram(),
                    line.metalValue(), line.makingCharge(), line.lineTotal()));
        }

        Invoice saved = repository.saveAndFlush(invoice);
        saved.assignInvoiceNumber(String.format(INVOICE_NUMBER_FORMAT, saved.getId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id " + id)));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getByInvoiceNumber(String invoiceNumber) {
        return toResponse(repository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with number " + invoiceNumber)));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByOrderId(Long orderId) {
        return repository.findByOrderId(orderId).stream().map(this::toResponse).toList();
    }

    /** The one non-cancelled invoice of an order. Note that a PAID invoice is still active. */
    @Transactional(readOnly = true)
    public InvoiceResponse getActiveByOrderId(Long orderId) {
        return toResponse(repository.findByOrderIdAndStatusNot(orderId, InvoiceStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active invoice found for order " + orderId)));
    }

    /**
     * Locks the invoice row until the caller's transaction ends. MANDATORY makes it fail when
     * the caller has no transaction, because the lock would be released immediately.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InvoiceResponse getByIdForUpdate(Long id) {
        return toResponse(findForUpdate(id));
    }

    /** GENERATED to PAID or CANCELLED. Cancelling runs every registered cancellation check. */
    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatus newStatus) {
        if (newStatus == InvoiceStatus.GENERATED) {
            throw new BusinessValidationException(
                    "An invoice can only be set back to GENERATED through the refund flow");
        }
        Invoice invoice = findForUpdate(id);
        if (newStatus == InvoiceStatus.CANCELLED) {
            InvoiceResponse current = toResponse(invoice);
            cancellationChecks.orderedStream().forEach(check -> check.assertCanCancel(current));
        }
        invoice.changeStatus(newStatus);
        return toResponse(invoice);
    }

    /** For the Payment module: PAID back to GENERATED after a refund drops the net paid amount. */
    @Transactional
    public InvoiceResponse markUnpaid(Long id) {
        Invoice invoice = findForUpdate(id);
        invoice.changeStatus(InvoiceStatus.GENERATED);
        return toResponse(invoice);
    }

    private Invoice findForUpdate(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id " + id));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> items = invoice.getItems().stream()
                .map(i -> new InvoiceItemResponse(i.getProductId(), i.getNetWeight(),
                        i.getRatePerGram(), i.getMetalValue(), i.getMakingCharge(), i.getLineTotal()))
                .toList();
        return new InvoiceResponse(invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getCustomerId(), invoice.getOrderId(), invoice.getStatus(),
                invoice.getCreatedAt(), items, invoice.getSubtotal(), invoice.getDiscountAmount(),
                invoice.getTaxableAmount(), invoice.getTaxAmount(), invoice.getGrandTotal());
    }
}