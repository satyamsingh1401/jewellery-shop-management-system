package com.myhourly.jewellery.billing.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myhourly.jewellery.billing.dto.InvoiceItemResponse;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceHtmlRendererTest {

    @Test
    void rendersInvoiceNumberAndGrandTotal() {
        InvoiceItemResponse item = new InvoiceItemResponse(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), new BigDecimal("60000.00"),
                new BigDecimal("5000.00"), new BigDecimal("65000.00"));
        InvoiceResponse invoice = new InvoiceResponse(1L, "INV-000001", 1L, 100L,
                InvoiceStatus.GENERATED, LocalDateTime.now(), List.of(item),
                new BigDecimal("65000.00"), new BigDecimal("6500.00"),
                new BigDecimal("58500.00"), new BigDecimal("1755.00"), new BigDecimal("60255.00"));

        String html = new InvoiceHtmlRenderer().render(invoice);

        assertTrue(html.contains("INV-000001"));
        assertTrue(html.contains("60255.00"));
    }
}