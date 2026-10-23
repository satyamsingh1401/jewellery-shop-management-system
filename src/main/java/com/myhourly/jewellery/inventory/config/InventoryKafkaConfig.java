package com.myhourly.jewellery.inventory.config;

import com.myhourly.jewellery.inventory.exception.InsufficientStockException;
import com.myhourly.jewellery.inventory.exception.InventoryNotFoundException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class InventoryKafkaConfig {

    /** Created automatically on startup if it does not exist (fine for development). */
    @Bean
    public NewTopic inventoryEventsTopic(@Value("${inventory.kafka.low-stock-topic:inventory-events}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    /**
     * Retry policy: a failing message is retried 3 more times, 1 second apart.
     * After that it is copied to "<topic>.DLT" (dead-letter topic) so it never blocks
     * the messages behind it and a human can inspect/replay it.
     * Errors that can never succeed by retrying go to the DLT immediately.
     * Spring Boot automatically uses this handler for @KafkaListener methods.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> template) {
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,      // malformed event
                InsufficientStockException.class,    // retrying will not create stock
                InventoryNotFoundException.class);   // product has no inventory row
        return handler;
    }
}
