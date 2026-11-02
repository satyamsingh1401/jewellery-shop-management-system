package com.myhourly.jewellery.service;


import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myhourly.jewellery.dto.NotificationRequest;
import com.myhourly.jewellery.entity.Notification;
import com.myhourly.jewellery.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;
	
	@Transactional
	public boolean createFromEvent(String eventId, String recipient,String type, String message) {
		if(eventId==null || eventId.isBlank()) {
			throw new IllegalArgumentException("Event ID is required");
		}
		
		if(notificationRepository.existsByEventId(eventId)) {
			System.out.println("Duplicate event skipped: " +eventId);
			return false;
		}
		
		Notification notification = new Notification();
		notification.setEventId(eventId);
		notification.setRecipient(recipient);
		notification.setType(type);
		notification.setMessage(message);
		notification.setStatus("CREATED");
		notification.setAttemptCount(0);
		
		try {
			
			notificationRepository.saveAndFlush(notification);
			return true;
			
		} catch (DataIntegrityViolationException e) {
			throw e;
		}
		
	}
	
	@Transactional
	public Notification create( String recipient, String type, String message ) {
		
		Notification notification = new Notification();
		
		notification.setRecipient(recipient);
		notification.setType(type);
		notification.setMessage(message);
		notification.setStatus("CREATED");
		
		 Notification save = notificationRepository.save(notification);
		 System.out.println("Notification Saved!!"+ save.getMessage());
		 
		 return save;
		
	}
	
	public Notification createManual(NotificationRequest request) {
		return create(request.getRecipient(),
				request.getType(),
				request.getMessage());
	}
	
	public List<Notification> getAll(){
		return notificationRepository.findAllByOrderByCreatedAtDesc();
	}
	
	public Notification getById(Long id) {
		return notificationRepository.findById(id).orElseThrow(()-> 
		new IllegalArgumentException("Notification not found: "+id));
	}
	
	public List<Notification> getByRecipient(String recipient){
		return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
	}
	
	public List<Notification> getByType(String type){
		return notificationRepository.findByTypeOrderByCreatedAtDesc(type);
	}

}
