package com.myhourly.jewellery.billing.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvoiceStatusTest {

    private Invoice newInvoice() {
        BigDecimal zero = BigDecimal.ZERO;
        return new Invoice("INV-TEST", 1L, null, zero, zero, zero, zero, zero);
    }

    @Test
    void generatedInvoiceCanBecomePaid() {
        Invoice invoice = newInvoice();
        invoice.changeStatus(InvoiceStatus.PAID);
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void paidInvoiceCannotBeCancelled() {
        Invoice invoice = newInvoice();
        invoice.changeStatus(InvoiceStatus.PAID);
        assertThrows(IllegalArgumentException.class,
                () -> invoice.changeStatus(InvoiceStatus.CANCELLED));
    }
}