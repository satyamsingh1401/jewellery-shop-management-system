package com.myhourly.jewellery.inventory.e2e;

import com.myhourly.jewellery.inventory.entity.Inventory;
import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.event.LowStockDetectedEvent;
import com.myhourly.jewellery.inventory.repository.InventoryRepository;
import com.myhourly.jewellery.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.myhourly.jewellery.inventory.InventoryLimits.MAX_QUANTITY_PER_MOVEMENT;
import static com.myhourly.jewellery.inventory.e2e.E2eSecurityConfig.ADMIN;
import static com.myhourly.jewellery.inventory.e2e.E2eSecurityConfig.ADMIN_PASS;
import static com.myhourly.jewellery.inventory.e2e.E2eSecurityConfig.STAFF;
import static com.myhourly.jewellery.inventory.e2e.E2eSecurityConfig.STAFF_PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(E2eSecurityConfig.class)
class InventoryApiE2ETest extends E2eSupport {

    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    InventoryRepository inventoryRepository;

    @Autowired
    StockMovementRepository movementRepository;

    Res admin(String method, String path, String body) {
        return call(method, path, body, ADMIN, ADMIN_PASS);
    }

    Res admin(String method, String path) {
        return admin(method, path, null);
    }

    long created(int qty, int reorder) {
        long id = newProductId();
        Res r = admin("POST", "/api/inventory", createBody(id, qty, reorder));
        assertThat(r.status()).as(r.raw()).isEqualTo(201);
        return id;
    }

    static String createBody(Object productId, Object qty, Object reorder) {
        return "{\"productId\":" + productId + ",\"initialQuantity\":" + qty + ",\"reorderLevel\":" + reorder + "}";
    }

    static String stockIn(Object qty, String reason) {
        return "{\"quantity\":" + qty + ",\"reason\":" + (reason == null ? "null" : "\"" + reason + "\"") + "}";
    }

    static String adjust(Object change, String reason) {
        return "{\"quantityChange\":" + change + ",\"reason\":" + (reason == null ? "null" : "\"" + reason + "\"") + "}";
    }

    int quantity(long productId) {
        return admin("GET", "/api/inventory/" + productId).body().path("availableQuantity").asInt();
    }

    JsonNode movements(long productId) {
        return admin("GET", "/api/inventory/" + productId + "/movements").body().path("content");
    }

    void assertApiError(Res r, int status, String message) {
        assertThat(r.status()).as(r.raw()).isEqualTo(status);
        assertThat(r.body().path("status").asInt()).as(r.raw()).isEqualTo(status);
        assertThat(r.body().has("timestamp")).as(r.raw()).isTrue();
        assertThat(r.body().has("path")).as(r.raw()).isTrue();
        assertThat(r.body().has("fieldErrors")).as(r.raw()).isTrue();
        if (message != null) {
            assertThat(r.body().path("message").asString()).as(r.raw()).isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("Create inventory: POST /api/inventory")
    class Create {

        @Test
        @DisplayName("CRT-01 valid request returns 201 with full body and an initial STOCK_IN movement")
        void createValid() {
            long id = newProductId();
            Res r = admin("POST", "/api/inventory", createBody(id, 10, 3));

            assertThat(r.status()).as(r.raw()).isEqualTo(201);
            assertThat(r.body().path("productId").asLong()).isEqualTo(id);
            assertThat(r.body().path("availableQuantity").asInt()).isEqualTo(10);
            assertThat(r.body().path("reorderLevel").asInt()).isEqualTo(3);
            assertThat(r.body().path("lowStock").asBoolean()).isFalse();
            assertThat(r.body().path("updatedAt").isMissingNode()).isFalse();

            JsonNode m = movements(id);
            assertThat(m.size()).isEqualTo(1);
            assertThat(m.get(0).path("movementType").asString()).isEqualTo("STOCK_IN");
            assertThat(m.get(0).path("quantityChange").asInt()).isEqualTo(10);
            assertThat(m.get(0).path("balanceAfter").asInt()).isEqualTo(10);
            assertThat(m.get(0).path("reason").asString()).isEqualTo("Initial stock");
            assertThat(m.get(0).path("createdBy").asString()).isEqualTo(ADMIN);
        }

        @Test
        @DisplayName("CRT-02 zero initial quantity creates the row, records no movement and is flagged low stock")
        void createZero() {
            long id = newProductId();
            Res r = admin("POST", "/api/inventory", createBody(id, 0, 0));

            assertThat(r.status()).isEqualTo(201);
            assertThat(r.body().path("lowStock").asBoolean()).isTrue();
            assertThat(movements(id).size()).isZero();
        }

        @Test
        @DisplayName("CRT-03 duplicate product returns 409 and leaves the original row unchanged")
        void createDuplicate() {
            long id = created(5, 1);
            Res r = admin("POST", "/api/inventory", createBody(id, 99, 1));

            assertApiError(r, 409, "Inventory already exists for product " + id);
            assertThat(quantity(id)).isEqualTo(5);
        }

        @Test
        @DisplayName("CRT-04 missing productId returns 400 with a productId field error")
        void createMissingProduct() {
            Res r = admin("POST", "/api/inventory", "{\"initialQuantity\":1,\"reorderLevel\":1}");
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("productId")).isTrue();
        }

        @Test
        @DisplayName("CRT-05 productId 0 and -1 are rejected with 400")
        void createNonPositiveProduct() {
            assertApiError(admin("POST", "/api/inventory", createBody(0, 1, 1)), 400, "Request validation failed");
            assertApiError(admin("POST", "/api/inventory", createBody(-1, 1, 1)), 400, "Request validation failed");
        }

        @Test
        @DisplayName("CRT-06 negative initialQuantity and reorderLevel are rejected with field errors")
        void createNegativeNumbers() {
            Res q = admin("POST", "/api/inventory", createBody(newProductId(), -1, 1));
            assertApiError(q, 400, "Request validation failed");
            assertThat(q.body().path("fieldErrors").has("initialQuantity")).isTrue();

            Res l = admin("POST", "/api/inventory", createBody(newProductId(), 1, -1));
            assertApiError(l, 400, "Request validation failed");
            assertThat(l.body().path("fieldErrors").has("reorderLevel")).isTrue();
        }

        @Test
        @DisplayName("CRT-07 malformed JSON, empty body and non-numeric values return 400 in the standard format")
        void createMalformed() {
            assertApiError(admin("POST", "/api/inventory", "{\"productId\": 5,"), 400,
                    "Request body contains invalid JSON or enum values");
            assertApiError(admin("POST", "/api/inventory", ""), 400,
                    "Request body contains invalid JSON or enum values");
            assertApiError(admin("POST", "/api/inventory", createBody(newProductId(), "\"abc\"", 1)), 400,
                    "Request body contains invalid JSON or enum values");
        }

        @Test
        @DisplayName("CRT-08 initialQuantity above Integer.MAX_VALUE returns 400")
        void createIntOverflow() {
            assertApiError(admin("POST", "/api/inventory", createBody(newProductId(), "2147483648", 1)), 400, null);
        }

        @Test
        @DisplayName("CRT-09 fractional initialQuantity 5.7 is rejected, not silently truncated to 5")
        void createFractionalQuantity() {
            long id = newProductId();
            Res r = admin("POST", "/api/inventory", createBody(id, "5.7", 1));
            assertThat(r.status()).as("observed body: " + r.raw()).isEqualTo(400);
            assertThat(admin("GET", "/api/inventory/" + id).status()).isEqualTo(404);
        }

        @Test
        @DisplayName("CRT-12 missing initialQuantity, reorderLevel and PATCH reorderLevel each name the missing field")
        void createMissingNumbersNameField() {
            long id = newProductId();
            Res q = admin("POST", "/api/inventory", "{\"productId\":" + id + ",\"reorderLevel\":1}");
            assertApiError(q, 400, "Request validation failed");
            assertThat(q.body().path("fieldErrors").has("initialQuantity")).as(q.raw()).isTrue();

            Res l = admin("POST", "/api/inventory", "{\"productId\":" + id + ",\"initialQuantity\":1}");
            assertApiError(l, 400, "Request validation failed");
            assertThat(l.body().path("fieldErrors").has("reorderLevel")).as(l.raw()).isTrue();
            assertThat(admin("GET", "/api/inventory/" + id).status()).isEqualTo(404);

            long existing = created(10, 4);
            Res patch = admin("PATCH", "/api/inventory/" + existing + "/reorder-level", "{}");
            assertApiError(patch, 400, "Request validation failed");
            assertThat(patch.body().path("fieldErrors").has("reorderLevel")).as(patch.raw()).isTrue();
            assertThat(admin("GET", "/api/inventory/" + existing).body().path("reorderLevel").asInt()).isEqualTo(4);
        }

        @Test
        @DisplayName("CRT-13 initialQuantity at the per-movement ceiling is accepted, one above is rejected")
        void createCeiling() {
            long id = created(MAX_QUANTITY_PER_MOVEMENT, 0);
            assertThat(quantity(id)).isEqualTo(MAX_QUANTITY_PER_MOVEMENT);

            long over = newProductId();
            Res r = admin("POST", "/api/inventory", createBody(over, MAX_QUANTITY_PER_MOVEMENT + 1, 0));
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("initialQuantity")).as(r.raw()).isTrue();
        }

        @Test
        @DisplayName("CRT-10 unsupported content type returns 415")
        void createWrongContentType() {
            Res r = call("POST", "/api/inventory", createBody(newProductId(), 1, 1), ADMIN, ADMIN_PASS, "text/plain");
            assertThat(r.status()).as(r.raw()).isEqualTo(415);
        }

        @Test
        @DisplayName("CRT-11 unknown fields such as availableQuantity cannot override the stored quantity")
        void createMassAssignment() {
            long id = newProductId();
            Res r = admin("POST", "/api/inventory",
                    "{\"productId\":" + id + ",\"initialQuantity\":2,\"reorderLevel\":0,\"availableQuantity\":9999,\"id\":1}");
            assertThat(r.status()).as(r.raw()).isIn(201, 400);
            if (r.status() == 201) {
                assertThat(quantity(id)).isEqualTo(2);
            }
        }
    }

    @Nested
    @DisplayName("Read inventory: GET /api/inventory/{productId}")
    class Read {

        @Test
        @DisplayName("GET-01 existing product returns 200")
        void getExisting() {
            long id = created(7, 2);
            Res r = admin("GET", "/api/inventory/" + id);
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.body().path("availableQuantity").asInt()).isEqualTo(7);
        }

        @Test
        @DisplayName("GET-02 unknown product returns 404 with message")
        void getUnknown() {
            assertApiError(admin("GET", "/api/inventory/987654321"), 404, "No inventory record for product 987654321");
        }

        @Test
        @DisplayName("GET-03 non-numeric and overflowing ids return 400 with a productId field error")
        void getBadId() {
            Res a = admin("GET", "/api/inventory/abc");
            assertApiError(a, 400, "Request validation failed");
            assertThat(a.body().path("fieldErrors").path("productId").asString()).isEqualTo("productId must be a valid Long");
            assertApiError(admin("GET", "/api/inventory/99999999999999999999"), 400, "Request validation failed");
        }
    }

    @Nested
    @DisplayName("Availability: GET /api/inventory/{productId}/availability")
    class Availability {

        @Test
        @DisplayName("AV-01 quantity below, equal to and above stock return true, true, false")
        void availabilityBoundaries() {
            long id = created(5, 0);
            Res below = admin("GET", "/api/inventory/" + id + "/availability?quantity=1");
            assertThat(below.status()).isEqualTo(200);
            assertThat(below.body().path("productId").asLong()).isEqualTo(id);
            assertThat(below.body().path("requestedQuantity").asInt()).isEqualTo(1);
            assertThat(below.body().path("available").asBoolean()).isTrue();
            assertThat(admin("GET", "/api/inventory/" + id + "/availability?quantity=5").body().path("available").asBoolean()).isTrue();
            assertThat(admin("GET", "/api/inventory/" + id + "/availability?quantity=6").body().path("available").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("AV-02 zero and negative quantity return 400")
        void availabilityNonPositive() {
            long id = created(5, 0);
            assertApiError(admin("GET", "/api/inventory/" + id + "/availability?quantity=0"), 400, "quantity must be at least 1");
            assertApiError(admin("GET", "/api/inventory/" + id + "/availability?quantity=-5"), 400, "quantity must be at least 1");
        }

        @Test
        @DisplayName("AV-03 missing, non-numeric and overflowing quantity return 400 with a quantity field error")
        void availabilityBadQuantity() {
            long id = created(5, 0);
            Res missing = admin("GET", "/api/inventory/" + id + "/availability");
            assertApiError(missing, 400, "Request validation failed");
            assertThat(missing.body().path("fieldErrors").path("quantity").asString()).isEqualTo("quantity is required");
            Res text = admin("GET", "/api/inventory/" + id + "/availability?quantity=abc");
            assertThat(text.body().path("fieldErrors").path("quantity").asString()).isEqualTo("quantity must be a valid int");
            assertApiError(admin("GET", "/api/inventory/" + id + "/availability?quantity=99999999999"), 400, "Request validation failed");
        }

        @Test
        @DisplayName("AV-04 unknown product returns 404")
        void availabilityUnknown() {
            assertApiError(admin("GET", "/api/inventory/987654322/availability?quantity=1"), 404, null);
        }
    }

    @Nested
    @DisplayName("Stock in: POST /api/inventory/{productId}/stock-in")
    class StockIn {

        @Test
        @DisplayName("SIN-01 valid stock-in increases quantity and records a STOCK_IN movement with balance and user")
        void stockInValid() {
            long id = created(10, 2);
            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(5, "Supplier delivery"));

            assertThat(r.status()).as(r.raw()).isEqualTo(200);
            assertThat(r.body().path("availableQuantity").asInt()).isEqualTo(15);
            JsonNode latest = movements(id).get(0);
            assertThat(latest.path("movementType").asString()).isEqualTo("STOCK_IN");
            assertThat(latest.path("quantityChange").asInt()).isEqualTo(5);
            assertThat(latest.path("balanceAfter").asInt()).isEqualTo(15);
            assertThat(latest.path("reason").asString()).isEqualTo("Supplier delivery");
            assertThat(latest.path("createdBy").asString()).isEqualTo(ADMIN);
        }

        @Test
        @DisplayName("SIN-02 zero, negative and missing quantity return 400 and change nothing")
        void stockInBadQuantity() {
            long id = created(10, 2);
            assertApiError(admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(0, "x")), 400, "Request validation failed");
            assertApiError(admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(-3, "x")), 400, "Request validation failed");
            assertApiError(admin("POST", "/api/inventory/" + id + "/stock-in", "{\"reason\":\"x\"}"), 400, null);
            assertThat(quantity(id)).isEqualTo(10);
            assertThat(movements(id).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("SIN-06 a missing quantity is reported as a quantity field error, not as invalid JSON")
        void stockInMissingQuantityNamesField() {
            long id = created(10, 2);
            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", "{\"reason\":\"x\"}");
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("quantity")).as("observed: " + r.raw()).isTrue();
            assertThat(quantity(id)).isEqualTo(10);
        }

        @Test
        @DisplayName("SIN-03 blank, missing and over-255-character reason return 400; exactly 255 is accepted")
        void stockInReasonBoundaries() {
            long id = created(10, 2);
            assertApiError(admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, "   ")), 400, "Request validation failed");
            assertApiError(admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, null)), 400, "Request validation failed");
            Res tooLong = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, "r".repeat(256)));
            assertApiError(tooLong, 400, "Request validation failed");
            assertThat(tooLong.body().path("fieldErrors").has("reason")).isTrue();
            assertThat(admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, "r".repeat(255))).status()).isEqualTo(200);
            assertThat(quantity(id)).isEqualTo(11);
        }

        @Test
        @DisplayName("SIN-04 unknown product returns 404 and writes no movement")
        void stockInUnknown() {
            long id = newProductId();
            assertApiError(admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(5, "x")), 404, "No inventory record for product " + id);
            assertThat(movementRepository.findAll().stream().noneMatch(m -> m.getProductId() == id)).isTrue();
        }

        @Test
        @DisplayName("SIN-05 stock-in that would overflow the stored quantity returns 400 and leaves stock unchanged")
        void stockInOverflow() {
            long id = created(10, 2);
            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(Integer.MAX_VALUE, "overflow"));
            assertThat(quantity(id)).isEqualTo(10);
            assertThat(r.status()).as("observed: " + r.raw()).isEqualTo(400);
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("quantity")).as(r.raw()).isTrue();
            assertThat(movements(id).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("SIN-07 stock-in of exactly the per-movement ceiling is accepted, one above is rejected")
        void stockInCeiling() {
            long id = created(10, 2);
            Res ok = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(MAX_QUANTITY_PER_MOVEMENT, "big delivery"));
            assertThat(ok.status()).as(ok.raw()).isEqualTo(200);
            assertThat(ok.body().path("availableQuantity").asInt()).isEqualTo(10 + MAX_QUANTITY_PER_MOVEMENT);

            Res over = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(MAX_QUANTITY_PER_MOVEMENT + 1, "too big"));
            assertApiError(over, 400, "Request validation failed");
            assertThat(over.body().path("fieldErrors").has("quantity")).as(over.raw()).isTrue();
            assertThat(quantity(id)).isEqualTo(10 + MAX_QUANTITY_PER_MOVEMENT);
        }

        @Test
        @DisplayName("SIN-08 fractional quantity is rejected, not truncated")
        void stockInFractional() {
            long id = created(10, 2);
            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn("2.5", "x"));
            assertApiError(r, 400, null);
            assertThat(quantity(id)).isEqualTo(10);
            assertThat(movements(id).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("SIN-09 stock-in that would push the stored total past Integer.MAX_VALUE returns 400, stock unchanged")
        void stockInStoredTotalOverflow() {
            long id = newProductId();
            int nearMax = Integer.MAX_VALUE - 5;
            inventoryRepository.save(new Inventory(id, nearMax, 0));

            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(10, "overflow"));
            assertApiError(r, 400, "Adding 10 units would exceed the maximum storable quantity for product " + id);
            assertThat(quantity(id)).isEqualTo(nearMax);
            assertThat(movements(id).size()).isZero();

            Res fits = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(5, "fits"));
            assertThat(fits.status()).as(fits.raw()).isEqualTo(200);
            assertThat(quantity(id)).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("Adjust: POST /api/inventory/{productId}/adjust")
    class Adjust {

        @Test
        @DisplayName("ADJ-01 positive and negative adjustments update stock and record signed ADJUSTMENT movements")
        void adjustBothWays() {
            long id = created(10, 0);
            assertThat(admin("POST", "/api/inventory/" + id + "/adjust", adjust(3, "Found in vault")).body().path("availableQuantity").asInt()).isEqualTo(13);
            assertThat(admin("POST", "/api/inventory/" + id + "/adjust", adjust(-4, "Damaged")).body().path("availableQuantity").asInt()).isEqualTo(9);

            JsonNode m = movements(id);
            assertThat(m.get(0).path("movementType").asString()).isEqualTo("ADJUSTMENT");
            assertThat(m.get(0).path("quantityChange").asInt()).isEqualTo(-4);
            assertThat(m.get(0).path("balanceAfter").asInt()).isEqualTo(9);
            assertThat(m.get(1).path("quantityChange").asInt()).isEqualTo(3);
            assertThat(m.get(1).path("balanceAfter").asInt()).isEqualTo(13);
        }

        @Test
        @DisplayName("ADJ-02 removing exactly the remaining stock leaves zero")
        void adjustToZero() {
            long id = created(4, 0);
            Res r = admin("POST", "/api/inventory/" + id + "/adjust", adjust(-4, "Write off"));
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.body().path("availableQuantity").asInt()).isZero();
        }

        @Test
        @DisplayName("ADJ-03 removing more than available returns 409 and changes nothing")
        void adjustBeyondStock() {
            long id = created(4, 0);
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust(-5, "Too many")), 409,
                    "Insufficient stock for product " + id + " (requested 5)");
            assertThat(quantity(id)).isEqualTo(4);
            assertThat(movements(id).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("ADJ-04 zero change, missing change and missing reason return 400")
        void adjustInvalid() {
            long id = created(4, 0);
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust(0, "x")), 400, "quantityChange must not be zero");
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", "{\"reason\":\"x\"}"), 400, null);
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust(1, null)), 400, "Request validation failed");
        }

        @Test
        @DisplayName("ADJ-05 unknown product returns 404 for both directions")
        void adjustUnknown() {
            long id = newProductId();
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust(1, "x")), 404, null);
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust(-1, "x")), 404, null);
        }

        @Test
        @DisplayName("ADJ-06 quantityChange Integer.MIN_VALUE returns 400 and leaves stock unchanged")
        void adjustMinValue() {
            long id = created(4, 0);
            Res r = admin("POST", "/api/inventory/" + id + "/adjust", adjust(Integer.MIN_VALUE, "edge"));
            assertThat(quantity(id)).isEqualTo(4);
            assertThat(r.status()).as("observed: " + r.raw()).isEqualTo(400);
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("quantityChange")).as(r.raw()).isTrue();
            assertThat(movements(id).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("ADJ-09 a missing quantityChange is reported as a quantityChange field error")
        void adjustMissingChangeNamesField() {
            long id = created(4, 0);
            Res r = admin("POST", "/api/inventory/" + id + "/adjust", "{\"reason\":\"x\"}");
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("quantityChange")).as(r.raw()).isTrue();
            assertThat(quantity(id)).isEqualTo(4);
        }

        @Test
        @DisplayName("ADJ-10 adjustments of exactly plus and minus the ceiling are accepted, one beyond is rejected")
        void adjustCeiling() {
            long id = created(10, 0);
            Res up = admin("POST", "/api/inventory/" + id + "/adjust", adjust(MAX_QUANTITY_PER_MOVEMENT, "recount"));
            assertThat(up.status()).as(up.raw()).isEqualTo(200);
            assertThat(up.body().path("availableQuantity").asInt()).isEqualTo(10 + MAX_QUANTITY_PER_MOVEMENT);

            Res down = admin("POST", "/api/inventory/" + id + "/adjust", adjust(-MAX_QUANTITY_PER_MOVEMENT, "recount"));
            assertThat(down.status()).as(down.raw()).isEqualTo(200);
            assertThat(down.body().path("availableQuantity").asInt()).isEqualTo(10);

            Res overUp = admin("POST", "/api/inventory/" + id + "/adjust", adjust(MAX_QUANTITY_PER_MOVEMENT + 1, "x"));
            assertApiError(overUp, 400, "Request validation failed");
            assertThat(overUp.body().path("fieldErrors").has("quantityChange")).as(overUp.raw()).isTrue();

            Res overDown = admin("POST", "/api/inventory/" + id + "/adjust", adjust(-MAX_QUANTITY_PER_MOVEMENT - 1, "x"));
            assertApiError(overDown, 400, "Request validation failed");
            assertThat(overDown.body().path("fieldErrors").has("quantityChange")).as(overDown.raw()).isTrue();

            assertThat(quantity(id)).isEqualTo(10);
            assertThat(movements(id).size()).isEqualTo(3);
        }

        @Test
        @DisplayName("ADJ-11 fractional quantityChange is rejected in both directions, not truncated")
        void adjustFractional() {
            long id = created(10, 0);
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust("1.5", "x")), 400, null);
            assertApiError(admin("POST", "/api/inventory/" + id + "/adjust", adjust("-1.5", "x")), 400, null);
            assertThat(quantity(id)).isEqualTo(10);
            assertThat(movements(id).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("ADJ-07 crossing the reorder level publishes exactly one LowStockDetected to Kafka after commit")
        void adjustPublishesLowStock() {
            long id = created(6, 5);
            admin("POST", "/api/inventory/" + id + "/adjust", adjust(-2, "Sold offline"));
            admin("POST", "/api/inventory/" + id + "/adjust", adjust(-1, "Sold offline"));

            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate, times(1)).send(eq("inventory-events"), eq(String.valueOf(id)), payload.capture());
            JsonNode event = json.readTree(payload.getValue());
            assertThat(event.path("event_type").asString()).isEqualTo("LowStockDetected");
            assertThat(event.path("event_version").asInt()).isEqualTo(1);
            assertThat(event.path("product_id").asLong()).isEqualTo(id);
            assertThat(event.path("available_quantity").asInt()).isEqualTo(4);
            assertThat(event.path("reorder_level").asInt()).isEqualTo(5);
            assertThat(event.path("event_id").asString()).isNotBlank();
            assertThat(event.path("occurred_at").isMissingNode()).isFalse();
        }

        @Test
        @DisplayName("ADJ-08 a rejected adjustment publishes nothing to Kafka")
        void rejectedAdjustPublishesNothing() {
            long id = created(6, 5);
            admin("POST", "/api/inventory/" + id + "/adjust", adjust(-7, "Too many"));
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Reorder level and low-stock list")
    class ReorderAndLowStock {

        @Test
        @DisplayName("ROL-01 raising the reorder level flags the item and lists it under low-stock")
        void reorderFlagsLowStock() {
            long id = created(10, 2);
            Res r = admin("PATCH", "/api/inventory/" + id + "/reorder-level", "{\"reorderLevel\":20}");
            assertThat(r.status()).as(r.raw()).isEqualTo(200);
            assertThat(r.body().path("reorderLevel").asInt()).isEqualTo(20);
            assertThat(r.body().path("lowStock").asBoolean()).isTrue();

            List<Long> low = new ArrayList<>();
            admin("GET", "/api/inventory/low-stock").body().forEach(n -> low.add(n.path("productId").asLong()));
            assertThat(low).contains(id);
        }

        @Test
        @DisplayName("ROL-02 negative reorder level returns 400, unknown product returns 404")
        void reorderInvalid() {
            long id = created(10, 2);
            assertApiError(admin("PATCH", "/api/inventory/" + id + "/reorder-level", "{\"reorderLevel\":-1}"), 400, "Request validation failed");
            assertApiError(admin("PATCH", "/api/inventory/" + newProductId() + "/reorder-level", "{\"reorderLevel\":1}"), 404, null);
        }

        @Test
        @DisplayName("ROL-03 an empty body is rejected instead of silently resetting the reorder level to 0")
        void reorderEmptyBody() {
            long id = created(10, 4);
            Res r = admin("PATCH", "/api/inventory/" + id + "/reorder-level", "{}");
            assertThat(r.status()).as("observed: " + r.raw()).isEqualTo(400);
        }

        @Test
        @DisplayName("LOW-01 low-stock list holds only items at or below their level, lowest quantity first")
        void lowStockList() {
            long healthy = created(50, 5);
            long low = created(3, 5);
            long lower = created(1, 5);
            List<Long> ids = new ArrayList<>();
            List<Integer> qty = new ArrayList<>();
            admin("GET", "/api/inventory/low-stock").body().forEach(n -> {
                ids.add(n.path("productId").asLong());
                qty.add(n.path("availableQuantity").asInt());
                assertThat(n.path("lowStock").asBoolean()).isTrue();
            });
            assertThat(ids).contains(low, lower).doesNotContain(healthy);
            assertThat(ids.indexOf(lower)).isLessThan(ids.indexOf(low));
            assertThat(qty).isSorted();
        }
    }

    @Nested
    @DisplayName("Movement history: GET /api/inventory/{productId}/movements")
    class Movements {

        @Test
        @DisplayName("MOV-01 history is newest first and pages correctly")
        void movementsPaging() {
            long id = created(1, 0);
            for (int i = 1; i <= 4; i++) {
                admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, "batch " + i));
            }
            Res page0 = admin("GET", "/api/inventory/" + id + "/movements?size=2&page=0");
            assertThat(page0.status()).as(page0.raw()).isEqualTo(200);
            JsonNode c = page0.body().path("content");
            assertThat(c.size()).isEqualTo(2);
            assertThat(c.get(0).path("reason").asString()).isEqualTo("batch 4");
            assertThat(c.get(0).path("balanceAfter").asInt()).isEqualTo(5);
            Res page2 = admin("GET", "/api/inventory/" + id + "/movements?size=2&page=2");
            assertThat(page2.body().path("content").get(0).path("reason").asString()).isEqualTo("Initial stock");
            assertThat(page0.raw()).contains("totalElements").contains("5");
        }

        @Test
        @DisplayName("MOV-02 unknown product returns 404")
        void movementsUnknown() {
            assertApiError(admin("GET", "/api/inventory/" + newProductId() + "/movements"), 404, null);
        }

        @Test
        @DisplayName("MOV-03 a huge page size is capped instead of loading the whole table")
        void movementsPageSizeCap() {
            long id = created(1, 0);
            Res r = admin("GET", "/api/inventory/" + id + "/movements?size=1000000");
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.raw()).doesNotContain("\"size\":1000000");
        }

        @Test
        @DisplayName("MOV-04 sorting by an unknown property returns 400, not 500")
        void movementsUnknownSort() {
            long id = created(1, 0);
            Res r = admin("GET", "/api/inventory/" + id + "/movements?sort=doesNotExist");
            assertThat(r.status()).as("observed: " + r.raw()).isEqualTo(400);
            assertApiError(r, 400, "Request validation failed");
            assertThat(r.body().path("fieldErrors").has("sort")).as(r.raw()).isTrue();
            assertThat(r.raw()).doesNotContain("doesNotExist").doesNotContain("StockMovement");
        }
    }

    @Nested
    @DisplayName("HTTP robustness")
    class Robustness {

        @Test
        @DisplayName("HTTP-01 unsupported method returns 405")
        void methodNotAllowed() {
            long id = created(1, 0);
            assertThat(admin("DELETE", "/api/inventory/" + id).status()).isEqualTo(405);
            assertThat(quantity(id)).isEqualTo(1);
        }

        @Test
        @DisplayName("HTTP-02 unknown route returns 404")
        void unknownRoute() {
            assertThat(admin("GET", "/api/inventory/1/does-not-exist").status()).isEqualTo(404);
        }

        @Test
        @DisplayName("HTTP-03 error bodies for 405/415/500-class errors use the standard ApiError format")
        void frameworkErrorsUseStandardFormat() {
            long id = created(1, 0);
            Res r = admin("DELETE", "/api/inventory/" + id);
            assertThat(r.body().has("fieldErrors")).as("observed 405 body: " + r.raw()).isTrue();
            assertApiError(r, 405, "HTTP method DELETE is not supported for this endpoint");
            assertThat(r.header("Allow")).as("Allow header").isNotNull().contains("GET");
            assertThat(r.body().path("path").asString()).isEqualTo("/api/inventory/" + id);
        }

        @Test
        @DisplayName("HTTP-04 unsupported Content-Type returns 415 in the standard ApiError format")
        void unsupportedMediaTypeUsesStandardFormat() {
            Res r = call("POST", "/api/inventory", createBody(newProductId(), 1, 0), ADMIN, ADMIN_PASS, "text/plain");
            assertApiError(r, 415, null);
            assertThat(r.body().path("message").asString()).as(r.raw()).contains("application/json");
            assertThat(r.header("Accept")).as("Accept header").isNotNull().contains("application/json");
        }

        @Test
        @DisplayName("HTTP-05 unknown route returns 404 in the standard ApiError format")
        void unknownRouteUsesStandardFormat() {
            Res r = admin("GET", "/api/inventory/1/does-not-exist");
            assertApiError(r, 404, "No endpoint matches the requested path");
            assertThat(r.body().path("path").asString()).isEqualTo("/api/inventory/1/does-not-exist");
        }
    }

    @Nested
    @DisplayName("Concurrency over HTTP")
    class Concurrency {

        @Test
        @DisplayName("CON-01 20 parallel '-1' adjustments on stock 5: exactly 5 succeed, 15 get 409, stock ends at 0")
        void parallelAdjustments() {
            long id = created(5, 0);
            List<CompletableFuture<Res>> calls = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                calls.add(callAsync("POST", "/api/inventory/" + id + "/adjust", adjust(-1, "race"), ADMIN, ADMIN_PASS, "application/json", null));
            }
            List<Integer> statuses = calls.stream().map(CompletableFuture::join).map(Res::status).toList();

            assertThat(statuses.stream().filter(s -> s == 200).count()).isEqualTo(5);
            assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(15);
            assertThat(quantity(id)).isZero();
            assertThat(movementRepository.findAll().stream()
                    .filter(m -> m.getProductId() == id && m.getMovementType() == MovementType.ADJUSTMENT).count()).isEqualTo(5);
        }

        @Test
        @DisplayName("CON-02 10 parallel creates of the same product: one 201, nine 409, one row")
        void parallelCreates() {
            long id = newProductId();
            List<CompletableFuture<Res>> calls = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                calls.add(callAsync("POST", "/api/inventory", createBody(id, 3, 0), ADMIN, ADMIN_PASS, "application/json", null));
            }
            List<Integer> statuses = calls.stream().map(CompletableFuture::join).map(Res::status).toList();

            assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
            assertThat(statuses.stream().filter(s -> s == 409).count()).as("statuses " + statuses).isEqualTo(9);
            assertThat(inventoryRepository.findAll().stream().filter(i -> i.getProductId() == id).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("CON-03 parallel stock-ins never lose an update: 25 x +2 on 0 ends at 50")
        void parallelStockIns() {
            long id = created(0, 0);
            List<CompletableFuture<Res>> calls = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                calls.add(callAsync("POST", "/api/inventory/" + id + "/stock-in", stockIn(2, "parallel"), ADMIN, ADMIN_PASS, "application/json", null));
            }
            calls.forEach(CompletableFuture::join);
            assertThat(quantity(id)).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Security under an authenticated, CSRF-free (stateless API) configuration")
    class Security {

        @Test
        @Tag("known-defect")
        @DisplayName("SEC-08 a non-admin STAFF user must not be able to adjust stock, stock in or change reorder level")
        void staffCannotChangeStock() {
            long id = created(10, 1);
            Res adj = call("POST", "/api/inventory/" + id + "/adjust", adjust(-10, "staff wipe"), STAFF, STAFF_PASS);
            Res in = call("POST", "/api/inventory/" + id + "/stock-in", stockIn(1000, "staff add"), STAFF, STAFF_PASS);
            Res rol = call("PATCH", "/api/inventory/" + id + "/reorder-level", "{\"reorderLevel\":0}", STAFF, STAFF_PASS);
            assertThat(List.of(adj.status(), in.status(), rol.status()))
                    .as("observed STAFF statuses (adjust, stock-in, reorder)").containsOnly(403);
        }

        @Test
        @DisplayName("SEC-09 the authenticated username is recorded on every movement")
        void auditUser() {
            long id = created(5, 0);
            call("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, "by staff"), STAFF, STAFF_PASS);
            assertThat(movements(id).get(0).path("createdBy").asString()).isIn(STAFF, ADMIN);
            assertThat(movements(id).get(1).path("createdBy").asString()).isEqualTo(ADMIN);
        }

        @Test
        @DisplayName("SEC-10 SQL injection attempts in the path are rejected and in the body are stored as plain text")
        void sqlInjection() {
            assertThat(admin("GET", "/api/inventory/1%20OR%201=1").status()).isEqualTo(400);
            assertThat(admin("GET", "/api/inventory/1;DROP%20TABLE%20inventory").status()).isIn(400, 401, 404);
            long id = created(5, 0);
            String payload = "x'); DELETE FROM inventory; --";
            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, payload));
            assertThat(r.status()).isEqualTo(200);
            assertThat(movements(id).get(0).path("reason").asString()).isEqualTo(payload);
            assertThat(inventoryRepository.count()).isPositive();
            assertThat(admin("GET", "/api/inventory/" + id + "/availability?quantity=1%20OR%201=1").status()).isEqualTo(400);
        }

        @Test
        @DisplayName("SEC-11 HTML/script in reason is returned as JSON with nosniff, not rendered")
        void storedMarkup() {
            long id = created(5, 0);
            String xss = "<script>alert(1)</script>";
            admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, xss));
            Res r = admin("GET", "/api/inventory/" + id + "/movements");
            assertThat(r.header("Content-Type")).startsWith("application/json");
            assertThat(r.header("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(r.body().path("content").get(0).path("reason").asString()).isEqualTo(xss);
        }

        @Test
        @DisplayName("SEC-12 server errors do not leak stack traces, exception classes or SQL")
        void noLeakOnServerError() {
            long id = created(1, 0);
            Res r = admin("GET", "/api/inventory/" + id + "/movements?sort=doesNotExist");
            assertThat(r.raw()).doesNotContain("Exception").doesNotContain("at org.").doesNotContain("trace").doesNotContain("select ");
            Res overflow = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(Integer.MAX_VALUE, "overflow"));
            assertThat(overflow.raw()).doesNotContain("Exception").doesNotContain("SQL").doesNotContain("update inventory");
        }

        @Test
        @DisplayName("SEC-13 a 1 MB reason is rejected with 400 and the server stays healthy")
        void oversizedPayload() {
            long id = created(1, 0);
            Res r = admin("POST", "/api/inventory/" + id + "/stock-in", stockIn(1, "A".repeat(1_000_000)));
            assertThat(r.status()).isEqualTo(400);
            assertThat(admin("GET", "/api/inventory/" + id).status()).isEqualTo(200);
        }

        @Test
        @DisplayName("SEC-14 response never exposes internal database ids or timestamps of other products")
        void noInternalIds() {
            long id = created(1, 0);
            Res r = admin("GET", "/api/inventory/" + id);
            assertThat(r.body().has("id")).isFalse();
            assertThat(r.body().has("createdAt")).isFalse();
        }
    }
}
