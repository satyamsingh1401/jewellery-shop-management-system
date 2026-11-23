package com.myhourly.jewellery.billing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class BillingControllerSecurityTest {

    @Test
    void priceCalculationIsOpenToAdminAndSalesStaff() throws Exception {
        Method method = BillingController.class.getMethod("calculate", BillCalculationRequest.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "calculate has no @PreAuthorize");
        assertEquals("hasAnyRole('ADMIN', 'SALES_STAFF')", annotation.value());
    }
}