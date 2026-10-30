package com.myhourly.jewellery.billing.service;

import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final String INVOICE_NUMBER_FORMAT = "INV-%06d";

    private final BillingCalculatorService calculator;
    private final InvoiceRepository repository;

    public InvoiceService(BillingCalculatorService calculator, InvoiceRepository repository) {
        this.calculator = calculator;
        this.repository = repository;
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
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

        // The INSERT runs now, so the database assigns the id.
        Invoice saved = repository.saveAndFlush(invoice);
        saved.assignInvoiceNumber(formatInvoiceNumber(saved.getId()));
        // The entity is managed, so Hibernate writes the number with an UPDATE when the
        // transaction commits. If anything fails, the whole invoice is rolled back.
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

    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatus newStatus) {
        Invoice invoice = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id " + id));
        invoice.changeStatus(newStatus);
        return toResponse(invoice);
    }

    private String formatInvoiceNumber(Long id) {
        return String.format(INVOICE_NUMBER_FORMAT, id);
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