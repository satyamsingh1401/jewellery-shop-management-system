package com.myhourly.jewellery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LowStockEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_version")
    private int eventVersion;

    @JsonProperty("occurred_at")
    private String occurredAt;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("available_quantity")
    private int availableQuantity;

    @JsonProperty("reorder_level")
    private int reorderLevel;
}

