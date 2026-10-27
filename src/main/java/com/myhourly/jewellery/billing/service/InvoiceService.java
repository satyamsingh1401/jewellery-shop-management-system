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
import com.myhourly.jewellery.billing.exception.InvoiceNotFoundException;
import com.myhourly.jewellery.billing.repository.InvoiceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;

@Service
public class InvoiceService {

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

        // Temporary unique number, replaced below once the database id is known.
        Invoice invoice = new Invoice(UUID.randomUUID().toString(), request.customerId(),
                request.orderId(), calc.subtotal(), calc.discount(), calc.taxableAmount(),
                calc.tax(), calc.grandTotal());

        for (int i = 0; i < request.items().size(); i++) {
            BillItemRequest in = request.items().get(i);
            BillItemResponse line = calc.items().get(i);
            invoice.addItem(new InvoiceItem(in.productId(), in.netWeight(), in.ratePerGram(),
                    line.metalValue(), line.makingCharge(), line.lineTotal()));
        }

        
        Invoice saved = repository.saveAndFlush(invoice);
        saved.assignInvoiceNumber(String.format("INV-%06d", saved.getId()));
        return toResponse(saved);
    }

        @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatus newStatus) {
        Invoice invoice = repository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with id " + id));
        invoice.changeStatus(newStatus);
        return toResponse(invoice);
    }
    
    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with id " + id)));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getByInvoiceNumber(String invoiceNumber) {
        return toResponse(repository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceNotFoundException(
                        "Invoice not found with number " + invoiceNumber)));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByOrderId(Long orderId) {
        return repository.findByOrderId(orderId).stream().map(this::toResponse).toList();
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