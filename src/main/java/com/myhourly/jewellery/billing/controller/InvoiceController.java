package com.myhourly.jewellery.billing.controller;

import com.myhourly.jewellery.billing.dto.InvoiceCreateRequest;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.dto.InvoiceStatusUpdateRequest;
import com.myhourly.jewellery.billing.service.InvoiceHtmlRenderer;
import com.myhourly.jewellery.billing.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceHtmlRenderer htmlRenderer;

    public InvoiceController(InvoiceService invoiceService, InvoiceHtmlRenderer htmlRenderer) {
        this.invoiceService = invoiceService;
        this.htmlRenderer = htmlRenderer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceCreateRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable Long id) {
        return invoiceService.getById(id);
    }

    @GetMapping("/by-number/{invoiceNumber}")
    public InvoiceResponse getByNumber(@PathVariable String invoiceNumber) {
        return invoiceService.getByInvoiceNumber(invoiceNumber);
    }

    @GetMapping
    public List<InvoiceResponse> getByOrder(@RequestParam Long orderId) {
        return invoiceService.getByOrderId(orderId);
    }

    @PatchMapping("/{id}/status")
    public InvoiceResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody InvoiceStatusUpdateRequest request) {
        return invoiceService.updateStatus(id, request.status());
    }

    @GetMapping("/{id}/print")
    public ResponseEntity<String> print(@PathVariable Long id) {
        String html = htmlRenderer.render(invoiceService.getById(id));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}