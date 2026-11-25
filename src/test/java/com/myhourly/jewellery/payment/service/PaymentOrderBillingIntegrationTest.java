package com.myhourly.jewellery.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.dto.InvoiceCreateRequest;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import com.myhourly.jewellery.billing.service.InvoiceService;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.order.entity.Order;
import com.myhourly.jewellery.order.repository.OrderRepository;
import com.myhourly.jewellery.order.service.impl.OrderPaymentStatusAdapter;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.enums.PaymentMethod;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// No enclosing test transaction: these assertions re-read committed data and
// verify that real service transaction failures roll back both tables.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentOrderBillingIntegrationTest {
    @Autowired PaymentService payments;
    @Autowired PaymentRepository paymentRepository;
    @Autowired InvoiceService invoices;
    @Autowired OrderRepository orders;
    @Autowired ApplicationContext context;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearTestDatabase() {
        for (String table : List.of("payments", "invoice_items", "invoices", "order_items", "orders", "customers")) {
            jdbc.update("delete from " + table);
        }
    }

    @Test
    void onlyRealOrderAdapterIsRegistered() {
        var updaters = context.getBeansOfType(OrderPaymentStatusUpdater.class);
        assertEquals(1, updaters.size());
        assertInstanceOf(OrderPaymentStatusAdapter.class, updaters.values().iterator().next());
    }

    @Test
    void loginPaymentAndRefundFlowUpdatesInvoiceAndOrder() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.test","password":"Test-only-password-123"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + json.readTree(login).get("accessToken").asText();
        String customer = mvc.perform(post("/api/customers").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"name":"Test Customer","phone":"9876543210",
                                 "email":"customer@example.test","address":"Test address"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long customerId = json.readTree(customer).get("id").asLong();
        String order = mvc.perform(post("/api/v1/orders").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"customerId":%d,"discountAmount":0,"items":[
                                {"productId":1,"quantity":1,"netWeight":1,"ratePerGramSnapshot":1000,
                                 "makingChargeSnapshot":0,"taxSnapshot":30}]}
                                """.formatted(customerId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long orderId = json.readTree(order).get("id").asLong();
        String invoice = mvc.perform(post("/api/billing/invoices").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"customerId":%d,"orderId":%d,"items":[{"productId":1,"netWeight":1,
                                "ratePerGram":1000,"makingChargeType":"FIXED","makingChargeValue":0}],
                                "discountType":"NONE","discountValue":0}
                                """.formatted(customerId, orderId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long invoiceId = json.readTree(invoice).get("id").asLong();
        long first = createOverHttp(auth, orderId, "400");
        long second = createOverHttp(auth, orderId, "630");
        mvc.perform(get("/api/payments/pending").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/payments/" + first + "/confirm").header("Authorization", auth))
                .andExpect(status().isOk());
        assertOrderStatus(orderId, "PARTIALLY_PAID");
        assertEquals(InvoiceStatus.GENERATED, invoices.getById(invoiceId).status());
        mvc.perform(post("/api/payments/" + second + "/confirm").header("Authorization", auth))
                .andExpect(status().isOk());
        assertOrderStatus(orderId, "PAID");
        assertEquals(InvoiceStatus.PAID, invoices.getById(invoiceId).status());
        refundOverHttp(auth, second, "130");
        assertOrderStatus(orderId, "PARTIALLY_PAID");
        assertEquals(InvoiceStatus.GENERATED, invoices.getById(invoiceId).status());
        refundOverHttp(auth, second, "500");
        refundOverHttp(auth, first, "400");
        assertOrderStatus(orderId, "REFUNDED");
        assertEquals(PaymentStatus.REFUNDED, payments.getPaymentById(first).getStatus());
        assertEquals(PaymentStatus.REFUNDED, payments.getPaymentById(second).getStatus());
        mvc.perform(get("/api/payments/pending").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingOrderRollsBackConfirmationAndInvoicePaidStatus() {
        InvoiceResponse invoice = invoiceFor(999999999L);
        long paymentId = create(invoice.orderId(), "1030", "staff@example.test");
        var error = assertThrows(BusinessValidationException.class, () -> payments.confirmPayment(paymentId));
        assertTrue(error.getMessage().contains("Order not found"));
        assertEquals(PaymentStatus.PENDING, payments.getPaymentById(paymentId).getStatus());
        assertNull(payments.getPaymentById(paymentId).getPaymentDate());
        assertEquals(InvoiceStatus.GENERATED, invoices.getById(invoice.id()).status());
    }

    @Test
    void missingOrderRollsBackRefundAndInvoiceUnpaidStatus() {
        Order order = order();
        InvoiceResponse invoice = invoiceFor(order.getId());
        long paymentId = create(order.getId(), "1030", "staff@example.test");
        payments.confirmPayment(paymentId);
        orders.deleteById(order.getId());
        assertThrows(BusinessValidationException.class,
                () -> payments.recordRefund(paymentId, new BigDecimal("1030"), "Return"));
        var payment = payments.getPaymentById(paymentId);
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(payment.getRefundedAmount()));
        assertEquals(InvoiceStatus.PAID, invoices.getById(invoice.id()).status());
    }

    @Test
    void longAuthenticatedEmailCanBePersisted() {
        Order order = order();
        invoiceFor(order.getId());
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(16) + ".test";
        assertEquals(150, email.length());
        long id = create(order.getId(), "100", email);
        assertEquals(email, paymentRepository.findById(id).orElseThrow().getCreatedBy());
    }

    @Test
    void pendingPaymentsReserveBalanceAndBlockInvoiceCancellation() {
        Order order = order();
        InvoiceResponse invoice = invoiceFor(order.getId());
        long id = create(order.getId(), "1030", "staff@example.test");
        assertThrows(BusinessValidationException.class, () -> create(order.getId(), "1", "staff@example.test"));
        assertThrows(BusinessValidationException.class,
                () -> invoices.updateStatus(invoice.id(), InvoiceStatus.CANCELLED));
        payments.cancelPayment(id);
        assertEquals(InvoiceStatus.CANCELLED, invoices.updateStatus(invoice.id(), InvoiceStatus.CANCELLED).status());
    }

    private long createOverHttp(String auth, long orderId, String amount) throws Exception {
        String response = mvc.perform(post("/api/payments").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"orderId":%d,"amount":%s,"paymentMethod":"UPI"}
                                """.formatted(orderId, amount)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private void refundOverHttp(String auth, long id, String amount) throws Exception {
        mvc.perform(post("/api/payments/" + id + "/refund").header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":" + amount + ",\"reason\":\"Customer return\"}"))
                .andExpect(status().isOk());
    }

    private void assertOrderStatus(long id, String status) {
        assertEquals(status, orders.findById(id).orElseThrow().getPaymentStatus().name());
    }

    private long create(Long orderId, String amount, String recorder) {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(orderId);
        request.setAmount(new BigDecimal(amount));
        request.setPaymentMethod(PaymentMethod.UPI);
        return payments.createPayment(request, recorder).getId();
    }

    private InvoiceResponse invoiceFor(Long orderId) {
        return invoices.createInvoice(new InvoiceCreateRequest(1L, orderId,
                List.of(new BillItemRequest(1L, BigDecimal.ONE, new BigDecimal("1000"),
                        MakingChargeType.FIXED, BigDecimal.ZERO)), DiscountType.NONE, BigDecimal.ZERO));
    }

    private Order order() {
        Order order = new Order();
        order.setOrderNumber("TEST-" + UUID.randomUUID());
        order.setCustomerId(1L);
        order.setSubtotalAmount(new BigDecimal("1000"));
        order.setTaxAmount(new BigDecimal("30"));
        order.setTotalAmount(new BigDecimal("1030"));
        return orders.save(order);
    }
}
