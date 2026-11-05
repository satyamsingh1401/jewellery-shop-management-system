package com.myhourly.jewellery.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.myhourly.jewellery.dto.LowStockEvent;
import com.myhourly.jewellery.dto.OrderEvent;
import com.myhourly.jewellery.service.NotificationService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
	
	private final ObjectMapper objectMapper;
	private final NotificationService notificationService;
	
	@Value("${notification.low-stock.recipient}")
	private String lowStockRecipient;
	
	@RetryableTopic(attempts = "3",
					backOff = @BackOff (delay=1000,multiplier = 2.0),
					topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
	@KafkaListener(topics = "${app.kafka.order-topic}",
				   groupId = "${spring.kafka.consumer.group-id}")
	public void consumeOrderEvent(@Payload String json) {
		
		OrderEvent event = objectMapper.readValue(json, OrderEvent.class);
		
		if(event.getEventId()==null || event.getEventId().isBlank()) {
			throw new IllegalArgumentException("Event ID is required");
		}
		
		if(event.getOrderId()==null || 
		   event.getCustomerEmail()==null || 
		   event.getCustomerEmail().isBlank()) {
			
			throw new IllegalArgumentException("Order ID and customer email are required");
		}
		
		
		String status = event.getStatus();
		
		if(!"CREATED".equalsIgnoreCase(status) && !"CONFIRMED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(status)) {
			throw new IllegalArgumentException("Unsupported order status: " + status);
		}
		
		notificationService.createFromEvent(
		        event.getEventId(),
		        event.getCustomerEmail(),
		        "ORDER_" + status.toUpperCase(),
		        "Your jewellery order " + event.getOrderId()
		            + " is " + status.toLowerCase() + "."
		    );
		
		
	}
	

@RetryableTopic(
    attempts = "3",
    backOff = @BackOff(delay = 1000, multiplier = 2.0),
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
)
@KafkaListener(
    topics = "${app.kafka.low-stock-topic}",
    groupId = "${spring.kafka.consumer.group-id}"
)
public void consumeLowStockEvent(@Payload String json) {

    LowStockEvent event =
            objectMapper.readValue(json, LowStockEvent.class);

    if (event.getEventId() == null || event.getEventId().isBlank()) {
        throw new IllegalArgumentException("Event ID is required");
    }

    if (!"LowStockDetected".equals(event.getEventType())) {
        throw new IllegalArgumentException("Unsupported event type");
    }

    if (event.getEventVersion() != 1) {
        throw new IllegalArgumentException("Unsupported event version");
    }

    if (event.getProductId() == null
            || event.getAvailableQuantity() < 0
            || event.getReorderLevel() < 0) {

        throw new IllegalArgumentException(
                "Invalid product or stock information"
        );
    }

    if (event.getAvailableQuantity() > event.getReorderLevel()) {
        throw new IllegalArgumentException(
                "Available quantity is above reorder level"
        );
    }

    notificationService.createFromEvent(
            event.getEventId(),
            lowStockRecipient,
            "LOW_STOCK",
            "Low stock alert: Product ID " + event.getProductId()
                    + ". Available quantity: " + event.getAvailableQuantity()
                    + ", reorder level: " + event.getReorderLevel() + "."
    );
}


}
