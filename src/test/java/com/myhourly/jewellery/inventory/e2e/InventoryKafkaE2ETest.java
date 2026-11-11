package com.myhourly.jewellery.inventory.e2e;

import com.myhourly.jewellery.common.exception.InsufficientStockException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.entity.StockMovement;
import com.myhourly.jewellery.inventory.kafka.OrderEventConsumer;
import com.myhourly.jewellery.inventory.repository.ProcessedEventRepository;
import com.myhourly.jewellery.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.myhourly.jewellery.inventory.e2e.E2eSecurityConfig.ADMIN;
import static com.myhourly.jewellery.inventory.e2e.E2eSecurityConfig.ADMIN_PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(E2eSecurityConfig.class)
class InventoryKafkaE2ETest extends E2eSupport {

    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    OrderEventConsumer consumer;

    @Autowired
    StockMovementRepository movementRepository;

    @Autowired
    ProcessedEventRepository processedEventRepository;

    long created(int qty, int reorder) {
        long id = newProductId();
        Res r = call("POST", "/api/inventory",
                "{\"productId\":" + id + ",\"initialQuantity\":" + qty + ",\"reorderLevel\":" + reorder + "}", ADMIN, ADMIN_PASS);
        assertThat(r.status()).as(r.raw()).isEqualTo(201);
        return id;
    }

    int quantity(long productId) {
        return call("GET", "/api/inventory/" + productId, null, ADMIN, ADMIN_PASS).body().path("availableQuantity").asInt();
    }

    static String event(String eventId, String type, String orderId, String items) {
        return "{\"event_id\":\"" + eventId + "\",\"event_type\":\"" + type + "\",\"order_id\":\"" + orderId
                + "\",\"items\":" + items + "}";
    }

    static String item(long productId, int qty) {
        return "{\"product_id\":" + productId + ",\"quantity\":" + qty + "}";
    }

    static String id() {
        return UUID.randomUUID().toString();
    }

    List<StockMovement> movements(long productId, MovementType type) {
        return movementRepository.findAll().stream()
                .filter(m -> m.getProductId() == productId && m.getMovementType() == type).toList();
    }

    @Test
    @DisplayName("KAF-01 OrderConfirmed deducts every line and records SALE movements referencing the order")
    void confirmDeducts() {
        long a = created(10, 0);
        long b = created(5, 0);
        String evt = id();
        String order = "ORD-" + id();

        consumer.consume(event(evt, "OrderConfirmed", order, "[" + item(a, 3) + "," + item(b, 2) + "]"));

        assertThat(quantity(a)).isEqualTo(7);
        assertThat(quantity(b)).isEqualTo(3);
        StockMovement sale = movements(a, MovementType.SALE).get(0);
        assertThat(sale.getQuantityChange()).isEqualTo(-3);
        assertThat(sale.getBalanceAfter()).isEqualTo(7);
        assertThat(sale.getReferenceType()).isEqualTo("ORDER");
        assertThat(sale.getReferenceId()).isEqualTo(order);
        assertThat(sale.getCreatedBy()).isEqualTo("system:kafka");
        assertThat(processedEventRepository.existsById(evt)).isTrue();
    }

    @Test
    @DisplayName("KAF-02 redelivery of the same event_id is ignored")
    void duplicateEventIgnored() {
        long a = created(10, 0);
        String msg = event(id(), "OrderConfirmed", "ORD-" + id(), "[" + item(a, 4) + "]");
        consumer.consume(msg);
        consumer.consume(msg);
        assertThat(quantity(a)).isEqualTo(6);
    }

    @Test
    @DisplayName("KAF-03 a second OrderConfirmed for the same order with a new event_id does not deduct again")
    void sameOrderNewEventIgnored() {
        long a = created(10, 0);
        String order = "ORD-" + id();
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 4) + "]"));
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 4) + "]"));
        assertThat(quantity(a)).isEqualTo(6);
        assertThat(movements(a, MovementType.SALE)).hasSize(1);
    }

    @Test
    @DisplayName("KAF-04 OrderCancelled restores exactly what was deducted, ignoring the quantity in the cancel event")
    void cancelRestoresDeducted() {
        long a = created(10, 0);
        String order = "ORD-" + id();
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 500) + "]"));
        assertThat(quantity(a)).isEqualTo(10);
        assertThat(movements(a, MovementType.RETURN).get(0).getQuantityChange()).isEqualTo(3);
    }

    @Test
    @DisplayName("KAF-05 two OrderCancelled events with different ids restore only once")
    void doubleCancelRestoresOnce() {
        long a = created(10, 0);
        String order = "ORD-" + id();
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "]"));
        assertThat(quantity(a)).isEqualTo(10);
        assertThat(movements(a, MovementType.RETURN)).hasSize(1);
    }

    @Test
    @DisplayName("KAF-06 a cancel that arrives before its confirm must not let the late confirm deduct stock")
    void cancelBeforeConfirm() {
        long a = created(10, 0);
        String order = "ORD-" + id();
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 3) + "]"));
        assertThat(quantity(a)).as("stock after cancel-then-confirm of the same order").isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-06b a late confirm for a cancelled multi-line order deducts no line and records no SALE")
    void cancelBeforeConfirmMultiLine() {
        long a = created(10, 0);
        long b = created(5, 0);
        String order = "ORD-" + id();
        String confirmEvt = id();
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "," + item(b, 2) + "]"));
        consumer.consume(event(confirmEvt, "OrderConfirmed", order, "[" + item(a, 3) + "," + item(b, 2) + "]"));
        assertThat(quantity(a)).isEqualTo(10);
        assertThat(quantity(b)).isEqualTo(5);
        assertThat(movements(a, MovementType.SALE)).isEmpty();
        assertThat(movements(b, MovementType.SALE)).isEmpty();
        assertThat(movements(a, MovementType.RETURN)).isEmpty();
        assertThat(processedEventRepository.existsById(confirmEvt)).isTrue();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("KAF-06c cancel, late confirm, then a second cancel leave stock and movements untouched")
    void cancelConfirmCancel() {
        long a = created(10, 0);
        String order = "ORD-" + id();
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "]"));
        assertThat(quantity(a)).isEqualTo(10);
        assertThat(movements(a, MovementType.SALE)).isEmpty();
        assertThat(movements(a, MovementType.RETURN)).isEmpty();
    }

    @Test
    @DisplayName("KAF-06d a cancelled order confirmed again with a new event_id is not deducted a second time")
    void confirmCancelConfirm() {
        long a = created(10, 0);
        String order = "ORD-" + id();
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderCancelled", order, "[" + item(a, 3) + "]"));
        consumer.consume(event(id(), "OrderConfirmed", order, "[" + item(a, 3) + "]"));
        assertThat(quantity(a)).isEqualTo(10);
        assertThat(movements(a, MovementType.SALE)).hasSize(1);
        assertThat(movements(a, MovementType.RETURN)).hasSize(1);
    }

    @Test
    @DisplayName("KAF-07 multi-line order is all-or-nothing: one short line rolls back every line and publishes nothing")
    void allOrNothing() {
        long a = created(6, 5);
        long b = created(1, 0);
        String evt = id();
        String msg = event(evt, "OrderConfirmed", "ORD-" + id(), "[" + item(a, 2) + "," + item(b, 5) + "]");

        assertThatThrownBy(() -> consumer.consume(msg)).isInstanceOf(InsufficientStockException.class);
        assertThat(quantity(a)).isEqualTo(6);
        assertThat(quantity(b)).isEqualTo(1);
        assertThat(movements(a, MovementType.SALE)).isEmpty();
        assertThat(processedEventRepository.existsById(evt)).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("KAF-08 repeated lines for one product are merged into a single deduction")
    void linesMerged() {
        long a = created(10, 0);
        consumer.consume(event(id(), "OrderConfirmed", "ORD-" + id(), "[" + item(a, 2) + "," + item(a, 3) + "]"));
        assertThat(quantity(a)).isEqualTo(5);
        assertThat(movements(a, MovementType.SALE)).hasSize(1);
        assertThat(movements(a, MovementType.SALE).get(0).getQuantityChange()).isEqualTo(-5);
    }

    @Test
    @DisplayName("KAF-09 malformed JSON and wrong field types raise IllegalArgumentException (sent straight to DLT)")
    void malformed() {
        assertThatThrownBy(() -> consumer.consume("{not json")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume(
                "{\"event_id\":\"x\",\"event_type\":\"OrderConfirmed\",\"order_id\":\"o\",\"items\":[{\"product_id\":\"abc\",\"quantity\":1}]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("KAF-10 missing event_id, event_type, order_id or items is rejected")
    void missingFields() {
        assertThatThrownBy(() -> consumer.consume("{\"event_type\":\"OrderConfirmed\",\"order_id\":\"o\",\"items\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume("{\"event_id\":\"e\",\"order_id\":\"o\",\"items\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume("{\"event_id\":\"e\",\"event_type\":\"OrderConfirmed\",\"items\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume("{\"event_id\":\"e\",\"event_type\":\"OrderConfirmed\",\"order_id\":\"o\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag("known-defect")
    @DisplayName("KAF-20 a literal JSON null message is rejected as IllegalArgumentException (non-retryable), not NullPointerException")
    void nullMessage() {
        assertThatThrownBy(() -> consumer.consume("null")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("KAF-11 zero, negative or missing line quantity and missing product_id are rejected with no change")
    void badLines() {
        long a = created(10, 0);
        assertThatThrownBy(() -> consumer.consume(event(id(), "OrderConfirmed", "o", "[" + item(a, 0) + "]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume(event(id(), "OrderConfirmed", "o", "[" + item(a, -2) + "]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume(event(id(), "OrderConfirmed", "o", "[{\"product_id\":" + a + "}]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume(event(id(), "OrderConfirmed", "o", "[{\"quantity\":1}]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.consume(event(id(), "OrderConfirmed", "o", "[null]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(quantity(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-12 order for a product with no inventory raises ResourceNotFoundException")
    void unknownProduct() {
        String evt = id();
        assertThatThrownBy(() -> consumer.consume(event(evt, "OrderConfirmed", "o", "[" + item(newProductId(), 1) + "]")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(processedEventRepository.existsById(evt)).isFalse();
    }

    @Test
    @DisplayName("KAF-13 unrelated event types are ignored without changing stock")
    void unrelatedType() {
        long a = created(10, 0);
        consumer.consume(event(id(), "OrderShipped", "o", "[" + item(a, 3) + "]"));
        assertThat(quantity(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-14 an event type with different casing ('orderConfirmed') is not silently dropped")
    void caseVariantType() {
        long a = created(10, 0);
        String evt = id();
        Throwable thrown = null;
        try {
            consumer.consume(event(evt, "orderConfirmed", "ORD-" + id(), "[" + item(a, 3) + "]"));
        } catch (Throwable t) {
            thrown = t;
        }
        boolean deducted = quantity(a) == 7;
        boolean rejected = thrown != null && !processedEventRepository.existsById(evt);
        assertThat(deducted || rejected)
                .as("event was neither applied nor rejected; it was marked processed and ignored").isTrue();
    }

    @Test
    @DisplayName("KAF-14b case or whitespace variants of handled types are rejected, not recorded, and rejected again on replay")
    void caseVariantsRejected() {
        long a = created(10, 0);
        for (String type : List.of("orderConfirmed", "ORDERCONFIRMED", "ordercancelled", " OrderConfirmed")) {
            String evt = id();
            String msg = event(evt, type, "ORD-" + id(), "[" + item(a, 3) + "]");
            assertThatThrownBy(() -> consumer.consume(msg)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> consumer.consume(msg)).isInstanceOf(IllegalArgumentException.class);
            assertThat(processedEventRepository.existsById(evt)).as(type).isFalse();
        }
        assertThat(quantity(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-13b unrelated event types are not recorded as processed")
    void unrelatedTypeNotRecorded() {
        long a = created(10, 0);
        String evt = id();
        consumer.consume(event(evt, "OrderShipped", "ORD-" + id(), "[" + item(a, 3) + "]"));
        assertThat(processedEventRepository.existsById(evt)).isFalse();
        assertThat(quantity(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-15 event_id or order_id longer than 64 chars fails and leaves stock unchanged")
    void tooLongIds() {
        long a = created(10, 0);
        assertThatThrownBy(() -> consumer.consume(event("e".repeat(65), "OrderConfirmed", "o", "[" + item(a, 1) + "]")));
        assertThatThrownBy(() -> consumer.consume(event(id(), "OrderConfirmed", "o".repeat(65), "[" + item(a, 1) + "]")));
        assertThat(quantity(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-16 unknown extra fields in the event and items are ignored")
    void extraFieldsIgnored() {
        long a = created(10, 0);
        consumer.consume("{\"event_id\":\"" + id() + "\",\"event_type\":\"OrderConfirmed\",\"order_id\":\"ORD-" + id()
                + "\",\"customer\":{\"name\":\"x\"},\"total\":99.5,\"items\":[{\"product_id\":" + a
                + ",\"quantity\":1,\"price\":10}]}");
        assertThat(quantity(a)).isEqualTo(9);
    }

    @Test
    @DisplayName("KAF-17 a sale that crosses the reorder level publishes LowStockDetected once, keyed by product")
    void lowStockFromSale() {
        long a = created(6, 5);
        consumer.consume(event(id(), "OrderConfirmed", "ORD-" + id(), "[" + item(a, 1) + "]"));
        consumer.consume(event(id(), "OrderConfirmed", "ORD-" + id(), "[" + item(a, 1) + "]"));
        verify(kafkaTemplate, times(1)).send(org.mockito.ArgumentMatchers.eq("inventory-events"),
                org.mockito.ArgumentMatchers.eq(String.valueOf(a)), anyString());
    }

    @Test
    @DisplayName("KAF-18 OrderConfirmed with an empty items list changes nothing")
    void emptyItems() {
        long a = created(10, 0);
        consumer.consume(event(id(), "OrderConfirmed", "ORD-" + id(), "[]"));
        assertThat(quantity(a)).isEqualTo(10);
    }

    @Test
    @DisplayName("KAF-19 10 concurrent orders for the last piece: exactly one succeeds")
    void concurrentLastPiece() throws Exception {
        long a = created(1, 0);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(pool.submit(() -> {
                try {
                    go.await();
                    consumer.consume(event(id(), "OrderConfirmed", "ORD-" + id(), "[" + item(a, 1) + "]"));
                    ok.incrementAndGet();
                } catch (InsufficientStockException e) {
                    refused.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                }
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertThat(ok.get()).isEqualTo(1);
        assertThat(refused.get()).isEqualTo(9);
        assertThat(other.get()).isZero();
        assertThat(quantity(a)).isZero();
    }
}
