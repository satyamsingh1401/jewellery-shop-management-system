package com.myhourly.jewellery.inventory.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

class InventorySecurityE2ETest extends E2eSupport {

    private static final String USER = E2eSecurityConfig.STAFF;
    private static final String PASS = E2eSecurityConfig.STAFF_PASS;
    private static final String ADMIN = E2eSecurityConfig.ADMIN;
    private static final String ADMIN_PASS = E2eSecurityConfig.ADMIN_PASS;

    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("SEC-01 every inventory endpoint rejects anonymous calls with 401")
    void anonymousRejected() {
        long id = newProductId();
        assertThat(call("GET", "/api/inventory/" + id, null, null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/api/inventory/low-stock", null, null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/api/inventory/" + id + "/availability?quantity=1", null, null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/api/inventory/" + id + "/movements", null, null, null).status()).isEqualTo(401);
        assertThat(call("POST", "/api/inventory", "{\"productId\":" + id + ",\"initialQuantity\":1,\"reorderLevel\":0}", null, null).status()).isEqualTo(401);
        assertThat(call("POST", "/api/inventory/" + id + "/adjust", "{\"quantityChange\":-1,\"reason\":\"x\"}", null, null).status()).isEqualTo(401);
        Res r = call("GET", "/api/inventory/" + id, null, null, null);
        assertThat(r.header("WWW-Authenticate")).isNull();
        assertThat(r.body().path("status").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-02 wrong password and unknown user return 401")
    void badCredentials() {
        assertThat(call("GET", "/api/inventory/low-stock", null, USER, "wrong").status()).isEqualTo(401);
        assertThat(call("GET", "/api/inventory/low-stock", null, "intruder@e2e.test", PASS).status()).isEqualTo(401);
        assertThat(call("GET", "/api/inventory/low-stock", null, "' OR '1'='1", "' OR '1'='1").status()).isIn(400, 401);
    }

    @Test
    @DisplayName("SEC-03 a valid user can call read endpoints")
    void authenticatedRead() {
        assertThat(call("GET", "/api/inventory/low-stock", null, USER, PASS).status()).isEqualTo(200);
        assertThat(call("GET", "/api/inventory/" + newProductId(), null, USER, PASS).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("SEC-04 an authenticated API client can perform write operations (POST/PATCH) without a CSRF token")
    void authenticatedWriteWithoutCsrfToken() {
        long id = newProductId();
        Res create = call("POST", "/api/inventory", "{\"productId\":" + id + ",\"initialQuantity\":1,\"reorderLevel\":0}", ADMIN, ADMIN_PASS);
        Res patch = call("PATCH", "/api/inventory/" + id + "/reorder-level", "{\"reorderLevel\":1}", ADMIN, ADMIN_PASS);
        assertThat(create.status()).as("observed create: " + create.status() + " " + create.raw()).isEqualTo(201);
        assertThat(patch.status()).as("observed patch: " + patch.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("SEC-05 standard security headers are present")
    void securityHeaders() {
        Res r = call("GET", "/api/inventory/low-stock", null, USER, PASS);
        assertThat(r.header("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(r.header("X-Frame-Options")).isEqualTo("DENY");
        assertThat(r.header("Cache-Control")).contains("no-store");
    }

    @Test
    @DisplayName("SEC-06 the 401 response does not leak internals")
    void unauthorizedBodyClean() {
        Res r = call("GET", "/api/inventory/1", null, null, null);
        assertThat(r.raw()).doesNotContain("Exception").doesNotContain("trace");
    }

    @Test
    @DisplayName("SEC-07 a cross-origin preflight from an unknown site is not granted")
    void corsPreflight() {
        Res r = callWithOrigin("OPTIONS", "/api/inventory", null, null, "https://evil.example");
        assertThat(r.header("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    @DisplayName("SEC-15 no session cookie is issued to API clients")
    void noSessionCookie() {
        Res r = call("GET", "/api/inventory/low-stock", null, USER, PASS);
        assertThat(r.header("Set-Cookie")).as("observed Set-Cookie").isNull();
    }

    @Test
    @DisplayName("SEC-16 the default form login page is not served")
    void noLoginPage() {
        Res anonymous = call("GET", "/login", null, null, null);
        Res authenticated = call("GET", "/login", null, USER, PASS);
        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.raw()).doesNotContain("<form");
        assertThat(authenticated.status()).isEqualTo(404);
        assertThat(authenticated.raw()).doesNotContain("<form");
    }

    @Test
    @DisplayName("SEC-17 a firewall-rejected path for an authenticated user returns 400, not 401")
    void rejectedPathNotUnauthorized() {
        Res r = call("GET", "/api/inventory/1;DROP%20TABLE%20inventory", null, USER, PASS);
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.raw()).doesNotContain("Exception").doesNotContain("trace");
    }

    @Test
    @DisplayName("SEC-19 a SALES_STAFF user can read but cannot create inventory")
    void staffCannotCreate() {
        long id = newProductId();
        Res create = call("POST", "/api/inventory", "{\"productId\":" + id + ",\"initialQuantity\":1,\"reorderLevel\":0}", USER, PASS);
        assertThat(create.status()).isEqualTo(403);
        assertThat(call("GET", "/api/inventory/low-stock", null, USER, PASS).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("SEC-20 a tampered bearer token is rejected with 401")
    void tamperedToken() {
        String token = login(USER, PASS).body().path("accessToken").asString();
        assertThat(token).isNotBlank();
        Res valid = send("GET", "/api/inventory/low-stock", null, token, null, null).join();
        String[] parts = token.split("[.]");
        char first = parts[1].charAt(0);
        parts[1] = (first == 'A' ? 'B' : 'A') + parts[1].substring(1);
        Res tampered = send("GET", "/api/inventory/low-stock", null, String.join(".", parts), null, null).join();
        assertThat(valid.status()).isEqualTo(200);
        assertThat(tampered.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-18 a write by an authenticated user issues no session cookie")
    void noSessionCookieOnWrite() {
        long id = newProductId();
        Res r = call("POST", "/api/inventory", "{\"productId\":" + id + ",\"initialQuantity\":1,\"reorderLevel\":0}", ADMIN, ADMIN_PASS);
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.header("Set-Cookie")).isNull();
    }
}
