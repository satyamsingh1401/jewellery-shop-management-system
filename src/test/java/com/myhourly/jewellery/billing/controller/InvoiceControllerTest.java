package com.myhourly.jewellery.billing.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.myhourly.jewellery.billing.dto.InvoiceStatusUpdateRequest;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.service.InvoiceHtmlRenderer;
import com.myhourly.jewellery.billing.service.InvoiceService;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceHtmlRenderer htmlRenderer;

    private InvoiceController controller;

    @BeforeEach
    void setUp() {
        controller = new InvoiceController(invoiceService, htmlRenderer);
    }

    @Test
    void updateStatusRejectsPaid() {
        InvoiceStatusUpdateRequest request = new InvoiceStatusUpdateRequest(InvoiceStatus.PAID);

        assertThrows(BusinessValidationException.class, () -> controller.updateStatus(1L, request));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void updateStatusRejectsGenerated() {
        InvoiceStatusUpdateRequest request = new InvoiceStatusUpdateRequest(InvoiceStatus.GENERATED);

        assertThrows(BusinessValidationException.class, () -> controller.updateStatus(1L, request));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void updateStatusAllowsCancelled() {
        InvoiceStatusUpdateRequest request = new InvoiceStatusUpdateRequest(InvoiceStatus.CANCELLED);

        controller.updateStatus(1L, request);

        verify(invoiceService).updateStatus(1L, InvoiceStatus.CANCELLED);
    }
}