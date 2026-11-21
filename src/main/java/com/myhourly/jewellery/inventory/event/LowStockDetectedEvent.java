package com.myhourly.jewellery.inventory.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when stock crosses DOWN through the reorder level.
 * Used twice: first as an in-process Spring event (raised inside the transaction),
 * then sent to Kafka as JSON only after that transaction has committed.
 */
public class LowStockDetectedEvent {

    @JsonProperty("event_id")
    private final String eventId;

    @JsonProperty("event_type")
    private final String eventType;

    @JsonProperty("event_version")
    private final int eventVersion;

    @JsonProperty("occurred_at")
    private final Instant occurredAt;

    @JsonProperty("product_id")
    private final Long productId;

    @JsonProperty("available_quantity")
    private final int availableQuantity;

    @JsonProperty("reorder_level")
    private final int reorderLevel;

    public LowStockDetectedEvent(String eventId, String eventType, int eventVersion, Instant occurredAt,
                                 Long productId, int availableQuantity, int reorderLevel) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.occurredAt = occurredAt;
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
    }

    public static LowStockDetectedEvent of(Long productId, int availableQuantity, int reorderLevel) {
        return new LowStockDetectedEvent(UUID.randomUUID().toString(), "LowStockDetected", 1,
                Instant.now(), productId, availableQuantity, reorderLevel);
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReorderLevel() { return reorderLevel; }
}
