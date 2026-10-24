package com.myhourly.jewellery.inventory.service;

import com.myhourly.jewellery.inventory.dto.CreateInventoryRequest;
import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.event.OrderEvent;
import com.myhourly.jewellery.inventory.event.OrderItemPayload;
import com.myhourly.jewellery.inventory.exception.InsufficientStockException;
import com.myhourly.jewellery.inventory.repository.InventoryRepository;
import com.myhourly.jewellery.inventory.repository.ProcessedEventRepository;
import com.myhourly.jewellery.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Concurrency tests against a REAL MySQL database (design document, section 11:
 * "Two requests competing for the last item cannot both succeed").
 *
 * How it works:
 *  - Uses its own database "jewellery_test" (created automatically, tables created at
 *    start and dropped at the end), so your normal data in jewellery_db is never touched.
 *  - Username and password come from your local application.properties.
 *    Use another database with the environment variable TEST_DB_URL.
 *  - Kafka is NOT needed: the listener is switched off and KafkaTemplate is a mock.
 *  - Many threads start at the same moment and call the service directly.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/jewellery_test?createDatabaseIfNotExist=true}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class InventoryConcurrencyTest {

    private static final long PRODUCT = 1L;

    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired InventoryService inventoryService;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired StockMovementRepository movementRepository;
    @Autowired ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanTables() {
        movementRepository.deleteAll();
        processedEventRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    // ------------------------------------------------------------------ tests

    @Test
    void lastPieceCannotBeSoldTwice() throws Exception {
        createStock(1, 0);

        int[] result = runConfirmedOrdersAtTheSameTime(10, true);

        assertEquals(1, result[0], "exactly one order may succeed");
        assertEquals(9, result[1], "the other nine must be refused for insufficient stock");
        assertEquals(0, availableQuantity());
        assertEquals(1, saleMovements());
        assertEquals(1, processedEventRepository.count());
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString()); // one LowStockDetected
    }

    @Test
    void stockOfFiveSellsExactlyFive() throws Exception {
        createStock(5, 0);

        int[] result = runConfirmedOrdersAtTheSameTime(20, true);

        assertEquals(5, result[0], "exactly five orders may succeed");
        assertEquals(15, result[1], "fifteen must be refused");
        assertEquals(0, availableQuantity());
        assertEquals(5, saleMovements());
    }

    @Test
    void sameEventDeliveredAtTheSameTimeDeductsAtMostOnce() throws Exception {
        createStock(10, 0);

        // Ten threads process the SAME event id and order id (a duplicate Kafka delivery).
        // Some may fail with a duplicate-key or deadlock error - Kafka would retry those and
        // they would then be recognised as duplicates. What must never happen is a double deduction.
        runConfirmedOrdersAtTheSameTime(10, false);

        long sales = saleMovements();
        assertTrue(sales <= 1, "the order must be deducted at most once, but was " + sales);
        assertEquals(10 - sales, availableQuantity(), "stock must match the recorded movements");
        assertEquals(sales, processedEventRepository.count());
    }

    // ---------------------------------------------------------------- helpers

    private void createStock(int quantity, int reorderLevel) {
        inventoryService.createInventory(new CreateInventoryRequest(PRODUCT, quantity, reorderLevel), "test");
    }

    private int availableQuantity() {
        return inventoryRepository.findAvailableQuantity(PRODUCT).orElseThrow();
    }

    private long saleMovements() {
        return movementRepository.findAll().stream()
                .filter(m -> m.getMovementType() == MovementType.SALE)
                .count();
    }

    /**
     * Starts 'threads' threads that all wait on a latch and then process an OrderConfirmed event
     * for one piece of the product at the same moment.
     *
     * @param distinctOrders true = every thread has its own event id and order id,
     *                       false = all threads share one event id and order id
     * @return {number succeeded, number refused for insufficient stock}
     */
    private int[] runConfirmedOrdersAtTheSameTime(int threads, boolean distinctOrders) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch allReady = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int n = distinctOrders ? i : 0;
            futures.add(pool.submit(() -> {
                allReady.countDown();
                try {
                    go.await();
                    inventoryService.processOrderEvent(new OrderEvent(
                            "evt-" + n, "OrderConfirmed", "ORD-" + n,
                            List.of(new OrderItemPayload(PRODUCT, 1))));
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException e) {
                    refused.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            }));
        }

        allReady.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        if (distinctOrders) {
            assertTrue(unexpected.isEmpty(), "unexpected errors: " + unexpected);
        }
        return new int[]{succeeded.get(), refused.get()};
    }
}
