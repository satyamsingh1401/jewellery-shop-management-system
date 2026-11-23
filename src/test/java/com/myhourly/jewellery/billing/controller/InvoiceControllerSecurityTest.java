package com.myhourly.jewellery.billing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.myhourly.jewellery.billing.dto.InvoiceCreateRequest;
import com.myhourly.jewellery.billing.dto.InvoiceStatusUpdateRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class InvoiceControllerSecurityTest {

    private static final String STAFF = "hasAnyRole('ADMIN', 'SALES_STAFF')";
    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private String ruleOf(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = InvoiceController.class.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " has no @PreAuthorize");
        return annotation.value();
    }

    @Test
    void creatingAnInvoiceIsOpenToAdminAndSalesStaff() throws Exception {
        assertEquals(STAFF, ruleOf("create", InvoiceCreateRequest.class));
    }

    @Test
    void readingInvoicesIsOpenToAdminAndSalesStaff() throws Exception {
        assertEquals(STAFF, ruleOf("getById", Long.class));
        assertEquals(STAFF, ruleOf("getByNumber", String.class));
        assertEquals(STAFF, ruleOf("getByOrder", Long.class));
        assertEquals(STAFF, ruleOf("print", Long.class));
    }

    @Test
    void changingTheStatusIsAdminOnly() throws Exception {
        assertEquals(ADMIN_ONLY, ruleOf("updateStatus", Long.class, InvoiceStatusUpdateRequest.class));
    }
}