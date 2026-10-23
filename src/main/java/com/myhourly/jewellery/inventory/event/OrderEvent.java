package com.myhourly.jewellery.inventory.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The part of the Order module's event that Inventory needs (PROPOSED contract -
 * agree the exact field names with the Order module owner).
 * Unknown extra fields are ignored so the Order module can add fields freely.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("items")
    private List<OrderItemPayload> items;

    public OrderEvent() { }

    public OrderEvent(String eventId, String eventType, String orderId, List<OrderItemPayload> items) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.orderId = orderId;
        this.items = items;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public List<OrderItemPayload> getItems() { return items; }
    public void setItems(List<OrderItemPayload> items) { this.items = items; }
}
