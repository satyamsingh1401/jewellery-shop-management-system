package com.myhourly.jewellery.inventory.kafka;

import com.myhourly.jewellery.inventory.event.LowStockDetectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends LowStockDetected to Kafka ONLY AFTER the database transaction has committed.
 * If the transaction rolls back, nothing is sent - so Kafka never announces a stock
 * level that the database does not have.
 * Known limit: if the app crashes between commit and send, the event is lost.
 * The Transactional Outbox pattern (design doc, section 9.1) closes that gap - a later step.
 */
@Component
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final String topic;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   JsonMapper jsonMapper,
                                   @Value("${inventory.kafka.low-stock-topic:inventory-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(LowStockDetectedEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);
            // Message key = productId, so all events of one product stay in order.
            kafkaTemplate.send(topic, String.valueOf(event.getProductId()), json);
            log.info("Published LowStockDetected for product {}", event.getProductId());
        } catch (JacksonException e) {
            log.error("Could not serialize LowStockDetected for product {}", event.getProductId(), e);
        }
    }
}
