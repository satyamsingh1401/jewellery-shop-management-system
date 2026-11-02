package com.myhourly.jewellery.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.myhourly.jewellery.entity.Notification;
import com.myhourly.jewellery.repository.NotificationRepository;
import com.myhourly.jewellery.service.EmailDeliveryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.email-enabled", havingValue = "true")
public class NotificationRetryScheduler {
	
	private static final int MAX_ATTEMPTS=3;
	
	private final NotificationRepository notificationRepository;
	private final EmailDeliveryService emailDeliveryService;
	
	@Scheduled(fixedDelayString = "${app.notification.retry-delay-ms=30000}")
	public void retryPendingNotifications() {
		
		List<Notification> pending = notificationRepository.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
				List.of("CREATED", "RETRY_PENDING") , LocalDateTime.now());
		for(Notification notification : pending) {
			delivery(notification);
		}
		
	}

	private void delivery(Notification notification) {
		notification.setAttemptCount(notification.getAttemptCount()+1);
		
		try {
			
			emailDeliveryService.send(notification.getRecipient(),
									  notification.getType(), 
									  notification.getMessage());
			
			notification.setStatus("SENT");
			notification.setSentAt(LocalDateTime.now());
			notification.setLastError(null);
			notification.setNextAttemptAt(null);
			
			System.out.println( "Email sent for notification "+notification.getId());
		} 
		catch (Exception e) {
			String error = e.getMessage();
			
			if(error==null || error.isBlank()) {
				error = error.getClass().getSimpleName();
			}
			
			notification.setLastError(error.substring(0, Math.min(error.length(), 2000)));
			
			if(notification.getAttemptCount() >= MAX_ATTEMPTS) {
				notification.setStatus("FAILED");
				notification.setNextAttemptAt(null);
				
				System.err.println("Email delivery permanently failed for notification "+notification.getId());
			}else {
				notification.setStatus("RETRY_PENDING");
				
				long delaySeconds = Math.min(30L *(1L << (notification.getAttemptCount()-1)) , 3600L);
				
				notification.setNextAttemptAt(LocalDateTime.now().plusSeconds(delaySeconds));
				
				System.err.println( "Email failed; retry scheduled for notification "+notification.getId());
			}
		}
		notificationRepository.save(notification);
	}
	
}
