package com.myhourly.jewellery.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.myhourly.jewellery.dto.LowStockEvent;
import com.myhourly.jewellery.dto.NotificationRequest;
import com.myhourly.jewellery.dto.OrderEvent;
import com.myhourly.jewellery.entity.Notification;
import com.myhourly.jewellery.service.NotificationProducer;
import com.myhourly.jewellery.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
public class NotificationController {
	
	private final NotificationService notificationService;
	private final NotificationProducer notificationProducer;
	
	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public Notification create(@Valid @RequestBody NotificationRequest notification) {
		return notificationService.createManual(notification);
	}
	
	@GetMapping
	public List<Notification> getAll(){
		return notificationService.getAll();
	}
	@GetMapping("/{id}")
	public Notification getById(@PathVariable Long id) {
		try {
			
			return notificationService.getById(id);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND, e.getMessage()
		            );
		}
	}
	
	@GetMapping("/recipient")
	public List<Notification> getByRecipient(@RequestParam String email){
		return notificationService.getByRecipient(email);
	}
	
	@GetMapping("/type")
	public List<Notification> getByType(@RequestParam String type){
		return notificationService.getByType(type);
	}
	
	// Test endpoint: publishes an event to Kafka.
	
	@PostMapping("/test/order-event")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public String publishOrderEvent(@RequestBody OrderEvent event) {
		notificationProducer.publishOrderEvent(event);
		return "Order event submitted to Kafka";
		
	}
	
	@PostMapping("/test/low-stock-event")
	@ResponseStatus(code = HttpStatus.ACCEPTED)
	public String publishLowStockEvent(@RequestBody LowStockEvent event) {
		notificationProducer.publishLowStockEvent(event);
		return "Low-stock event submitted to Kafka";
	}
	

}
