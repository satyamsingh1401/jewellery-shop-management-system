package com.myhourly.jewellery.inventory.e2e;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:e2e-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "spring.security.user.name=e2e-user",
        "spring.security.user.password=e2e-pass"
})
abstract class E2eSupport {

    private static final AtomicLong PRODUCT_IDS = new AtomicLong(System.nanoTime() % 1_000_000 * 100 + 10_000);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    record Res(int status, String raw, JsonNode body, HttpHeaders headers) {
        String header(String name) {
            return headers.firstValue(name).orElse(null);
        }
    }

    static long newProductId() {
        return PRODUCT_IDS.incrementAndGet();
    }

    Res call(String method, String path, String body, String user, String password) {
        return callAsync(method, path, body, user, password, "application/json", null).join();
    }

    Res call(String method, String path, String body, String user, String password, String contentType) {
        return callAsync(method, path, body, user, password, contentType, null).join();
    }

    Res callWithOrigin(String method, String path, String user, String password, String origin) {
        return callAsync(method, path, null, user, password, "application/json", origin).join();
    }

    CompletableFuture<Res> callAsync(String method, String path, String body, String user, String password,
                                     String contentType, String origin) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(60))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (body != null && contentType != null) {
            builder.header("Content-Type", contentType);
        }
        builder.header("Accept", "application/json");
        if (user != null) {
            String token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
        if (origin != null) {
            builder.header("Origin", origin);
            builder.header("Access-Control-Request-Method", "POST");
        }
        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(r -> new Res(r.statusCode(), r.body(), parse(r.body()), r.headers()));
    }

    private JsonNode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return json.createObjectNode();
        }
        try {
            return json.readTree(raw);
        } catch (RuntimeException e) {
            return json.createObjectNode();
        }
    }
}
