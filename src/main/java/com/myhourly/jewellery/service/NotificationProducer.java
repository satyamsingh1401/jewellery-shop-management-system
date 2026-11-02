package com.myhourly.jewellery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.myhourly.jewellery.dto.LowStockEvent;
import com.myhourly.jewellery.dto.OrderEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationProducer {
	
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	
	@Value("${app.kafka.order-topic}")
	private String orderTopic;
	
	@Value("${app.kafka.low-stock-topic}")
	private String lowStockTopic;
	
	public void publishOrderEvent(OrderEvent event) {
		send(orderTopic,"order-"+event.getOrderId(),event);
	}
	public void publishLowStockEvent(LowStockEvent event) {
		send(lowStockTopic,"order-"+event.getProductId(),event);
	}
	private void send(String topic, String key, Object event) {
		
		String json = objectMapper.writeValueAsString(event);
		
		kafkaTemplate.send(topic, key, json).whenComplete((result,eception)->{
			if(eception!=null) {
				System.err.println("Kafka publish failed: "+eception.getMessage());
			}else {
				System.out.println("Published event to "+topic);
			}
		});
	}
	

}
