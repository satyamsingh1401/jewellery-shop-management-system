package com.myhourly.jewellery.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.dto.InvoiceCreateRequest;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.entity.Invoice;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import com.myhourly.jewellery.billing.repository.InvoiceRepository;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository repository;

    private InvoiceService service;

    @BeforeEach
    void setUp() {
        service = serviceWith();
    }

    private InvoiceService serviceWith(InvoiceCancellationCheck... checks) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        for (int i = 0; i < checks.length; i++) {
            factory.addBean("cancellationCheck" + i, checks[i]);
        }
        ObjectProvider<InvoiceCancellationCheck> provider =
                factory.getBeanProvider(InvoiceCancellationCheck.class);
        return new InvoiceService(new BillingCalculatorService(new BigDecimal("3")), repository, provider);
    }

    private BillItemRequest sampleItem() {
        return new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), MakingChargeType.PER_GRAM, new BigDecimal("500.00"));
    }

    private InvoiceCreateRequest sampleRequest() {
        return new InvoiceCreateRequest(
                1L, 100L, List.of(sampleItem()), DiscountType.PERCENTAGE, new BigDecimal("10"));
    }

    private Invoice invoiceWithId(long id, long orderId, String number) {
        BigDecimal zero = BigDecimal.ZERO;
        Invoice invoice = new Invoice(number, 1L, orderId, zero, zero, zero, zero, zero);
        ReflectionTestUtils.setField(invoice, "id", id);
        return invoice;
    }

    // ---------- creating invoices ----------

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

    @Test
    void rejectsSecondActiveInvoiceForTheSameOrder() {
        when(repository.existsByOrderIdAndStatusNot(100L, InvoiceStatus.CANCELLED)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createInvoice(sampleRequest()));

        verify(repository, never()).saveAndFlush(any(Invoice.class));
    }

    @Test
    void cancelledInvoicesDoNotBlockANewInvoiceAndNumbersAreNotReused() {
        when(repository.existsByOrderIdAndStatusNot(100L, InvoiceStatus.CANCELLED)).thenReturn(false);
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 8L);
            return invoice;
        });

        InvoiceResponse response = service.createInvoice(sampleRequest());

        assertEquals("INV-000008", response.invoiceNumber());
        verify(repository).existsByOrderIdAndStatusNot(100L, InvoiceStatus.CANCELLED);
    }

    @Test
    void skipsTheDuplicateCheckWhenTheInvoiceHasNoOrder() {
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 9L);
            return invoice;
        });
        InvoiceCreateRequest noOrder = new InvoiceCreateRequest(
                1L, null, List.of(sampleItem()), DiscountType.NONE, null);

        service.createInvoice(noOrder);

        verify(repository, never()).existsByOrderIdAndStatusNot(any(), any());
    }

    // ---------- reading invoices ----------

    @Test
    void throwsSharedNotFoundExceptionForUnknownInvoice() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void returnsTheActiveInvoiceForAnOrder() {
        when(repository.findByOrderIdAndStatusNot(100L, InvoiceStatus.CANCELLED))
                .thenReturn(Optional.of(invoiceWithId(10L, 100L, "INV-000010")));

        InvoiceResponse response = service.getActiveByOrderId(100L);

        assertEquals("INV-000010", response.invoiceNumber());
        assertEquals(InvoiceStatus.GENERATED, response.status());
    }

    @Test
    void throwsNotFoundWhenTheOrderHasNoActiveInvoice() {
        when(repository.findByOrderIdAndStatusNot(100L, InvoiceStatus.CANCELLED))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getActiveByOrderId(100L));
    }

    @Test
    void lockingReadReturnsTheInvoice() {
        when(repository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(invoiceWithId(5L, 100L, "INV-000005")));

        assertEquals("INV-000005", service.getByIdForUpdate(5L).invoiceNumber());
    }

    @Test
    void lockingReadThrowsNotFoundForAnUnknownInvoice() {
        when(repository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByIdForUpdate(999L));
    }

    // ---------- status changes ----------

    @Test
    void generatedInvoiceCanBecomePaid() {
        when(repository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(invoiceWithId(5L, 100L, "INV-000005")));

        assertEquals(InvoiceStatus.PAID, service.updateStatus(5L, InvoiceStatus.PAID).status());
    }

    @Test
    void cancellationSucceedsWhenNoCheckObjects() {
        when(repository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(invoiceWithId(5L, 100L, "INV-000005")));

        assertEquals(InvoiceStatus.CANCELLED, service.updateStatus(5L, InvoiceStatus.CANCELLED).status());
    }

    @Test
    void cancellationIsBlockedWhenAPaymentCheckRejectsIt() {
        InvoiceCancellationCheck blocking = invoice -> {
            throw new BusinessValidationException("Invoice has payments");
        };
        InvoiceService guarded = serviceWith(blocking);
        Invoice invoice = invoiceWithId(5L, 100L, "INV-000005");
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessValidationException.class,
                () -> guarded.updateStatus(5L, InvoiceStatus.CANCELLED));
        assertEquals(InvoiceStatus.GENERATED, invoice.getStatus());
    }

    @Test
    void publicStatusUpdateCannotSetGenerated() {
        assertThrows(BusinessValidationException.class,
                () -> service.updateStatus(5L, InvoiceStatus.GENERATED));
    }

    @Test
    void markUnpaidMovesAPaidInvoiceBackToGenerated() {
        Invoice invoice = invoiceWithId(6L, 100L, "INV-000006");
        invoice.changeStatus(InvoiceStatus.PAID);
        when(repository.findByIdForUpdate(6L)).thenReturn(Optional.of(invoice));

        assertEquals(InvoiceStatus.GENERATED, service.markUnpaid(6L).status());
    }

    @Test
    void markUnpaidRejectsAnInvoiceThatIsNotPaid() {
        when(repository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(invoiceWithId(7L, 100L, "INV-000007")));

        assertThrows(BusinessValidationException.class, () -> service.markUnpaid(7L));
    }
}