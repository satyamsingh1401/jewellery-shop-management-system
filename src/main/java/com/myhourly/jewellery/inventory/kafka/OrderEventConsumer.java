package com.myhourly.jewellery.inventory.kafka;

import com.myhourly.jewellery.inventory.event.OrderEvent;
import com.myhourly.jewellery.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Listens to the Order module's topic. Receives the raw JSON text and parses it
 * ourselves, so a malformed message gives a clear error (and goes to the dead-letter
 * topic) instead of breaking the Kafka deserializer.
 * Spring Boot 4 uses Jackson 3 (package tools.jackson); JsonMapper is the bean Boot provides.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final JsonMapper jsonMapper;
    private final InventoryService inventoryService;

    public OrderEventConsumer(JsonMapper jsonMapper, InventoryService inventoryService) {
        this.jsonMapper = jsonMapper;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "${inventory.kafka.order-events-topic:order-events}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service}")
    public void consume(String message) {
        OrderEvent event;
        try {
            event = jsonMapper.readValue(message, OrderEvent.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Malformed order event: " + e.getMessage(), e);
        }
        log.info("Received {} for order {}", event.getEventType(), event.getOrderId());
        inventoryService.processOrderEvent(event);
    }
}
