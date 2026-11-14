package com.myhourly.jewellery.service;


import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.myhourly.jewellery.dto.NotificationRequest;
import com.myhourly.jewellery.entity.Notification;
import com.myhourly.jewellery.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;
	

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public boolean createFromEvent(
        String eventId,
        String recipient,
        String type,
        String message) {

    if (eventId == null || eventId.isBlank()) {
        throw new IllegalArgumentException("Event ID is required");
    }

    // 1. Check whether the event was already processed
    if (notificationRepository.existsByEventId(eventId)) {
        System.out.println("Duplicate event skipped: " + eventId);
        return false;
    }

    // 2. Prepare the notification
    Notification notification = new Notification();

    notification.setEventId(eventId);
    notification.setRecipient(recipient);
    notification.setType(type);
    notification.setMessage(message);
    notification.setStatus("CREATED");
    notification.setAttemptCount(0);

    try {

        // 3. Save and flush immediately
        notificationRepository.saveAndFlush(notification);

        System.out.println("Notification created for event: " + eventId);

        return true;

    } catch (DataIntegrityViolationException exception) {

        // 4. Check whether another request saved the same event
        if (notificationRepository.existsByEventId(eventId)) {

            System.out.println(
                    "Concurrent duplicate event skipped: " + eventId
            );

            return false;
        }

        throw exception;
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
