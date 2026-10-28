package com.myhourly.jewellery.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.dto.InvoiceCreateRequest;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.entity.Invoice;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import com.myhourly.jewellery.billing.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository repository;

    private InvoiceService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(new BillingCalculatorService(new BigDecimal("3")), repository);
    }

    @Test
    void createsInvoiceWithGeneratedNumberAndCorrectTotals() {
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 7L);
            return invoice;
        });

        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), MakingChargeType.PER_GRAM, new BigDecimal("500.00"));
        InvoiceCreateRequest request = new InvoiceCreateRequest(
                1L, 100L, List.of(item), DiscountType.PERCENTAGE, new BigDecimal("10"));

        InvoiceResponse response = service.createInvoice(request);

        assertEquals("INV-000007", response.invoiceNumber());
        assertEquals(new BigDecimal("60255.00"), response.grandTotal());
        assertEquals(1, response.items().size());
    }
        @Test
    void createsInvoiceNumberFromDatabaseGeneratedId() {
        AtomicReference<String> numberAtInsert = new AtomicReference<>("not-captured");
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            numberAtInsert.set(invoice.getInvoiceNumber());
            ReflectionTestUtils.setField(invoice, "id", 7L);
            return invoice;
        });

        InvoiceResponse response = service.createInvoice(sampleRequest());

        assertNull(numberAtInsert.get());
        assertEquals("INV-000007", response.invoiceNumber());
        assertEquals(new BigDecimal("60255.00"), response.grandTotal());
        assertEquals(1, response.items().size());
    }

    @Test
    void invoiceNumberGrowsPastSixDigitsWithoutBreakingUniqueness() {
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 1234567L);
            return invoice;
        });

        assertEquals("INV-1234567", service.createInvoice(sampleRequest()).invoiceNumber());
    }

    private InvoiceCreateRequest sampleRequest() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), MakingChargeType.PER_GRAM, new BigDecimal("500.00"));
        return new InvoiceCreateRequest(
                1L, 100L, List.of(item), DiscountType.PERCENTAGE, new BigDecimal("10"));
    }
}