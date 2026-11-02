package com.myhourly.jewellery.consumer;

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
	
	@RetryableTopic(attempts = "3",
					backOff = @BackOff(delay = 1000,multiplier = 2.0),
					topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
	@KafkaListener(topics = "${app.kafka.low-stock-topic}",
				   groupId = "${spring.kafka.consumer.group-id}")
	public void consumeLowStockEvent(@Payload String json) {
		
		LowStockEvent event = objectMapper.readValue(json, LowStockEvent.class);
		
		if(event.getEventId()==null || event.getEventId().isBlank()) {
			throw new IllegalArgumentException("Event ID is required");
		}
		
		if(event.getProductId()==null ||
		   event.getProductName()==null ||
		   event.getProductName().isBlank() ||
		   event.getAvailableQuantity()<0 ||
		   event.getThreshold()<0) {
			
			throw new IllegalArgumentException("Invalid product or stock information");
		}
		
		if(event.getAvailableQuantity()<0 || event.getThreshold()<0) {
			throw new IllegalArgumentException("Stock quantity and threshold cannot be negative");
		}
		
		if(event.getAvailableQuantity()<event.getThreshold()) {
			
			notificationService.createFromEvent(
		            event.getEventId(),
		            "store-staff",
		            "LOW_STOCK",
		            "Low stock alert: " + event.getProductName()
		                + " (Product ID: " + event.getProductId()
		                + "). Available: " + event.getAvailableQuantity()
		                + ", threshold: " + event.getThreshold() + "."
		        );
		}
		
	}
	@DltHandler
	public void handleDeadLetter(@Payload String message,@Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		System.err.println("Event moved to retry/dead-letter topic "+topic+" : "+message);
	}

}
