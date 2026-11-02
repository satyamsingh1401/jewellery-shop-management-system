package com.myhourly.jewellery.repository;



import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myhourly.jewellery.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>  {
	
	
	
	List<Notification> findAllByOrderByCreatedAtDesc();
	List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
	List<Notification> findByTypeOrderByCreatedAtDesc(String type);
	
	boolean existsByEventId(String eventId);
	List<Notification> findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List<String> statuses, LocalDateTime now);

}
