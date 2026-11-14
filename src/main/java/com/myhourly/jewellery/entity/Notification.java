package com.myhourly.jewellery.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Notification {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "event_id", length = 100, unique = true)
	private String eventId;
	
	@Column(nullable = false, length = 225)
	private String recipient;
	
	@Column(nullable = false, length = 50)
	private String type;
	
	@Column(nullable = false, length = 1000)
	private String message;
	
	@Column(nullable = false, length = 30)
	private String status = "CREATED";
	
	@Column(nullable = false)
	private int attemptCount=0;
	
	@Column(length = 2000)
	private String lastError;
	
	
	private LocalDateTime nextAttemptAt;
	
	private LocalDateTime sentAt;
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@PrePersist
	public void onCreate() {
		
		if(createdAt==null) {
			createdAt = LocalDateTime.now();
		}
		if (status==null) {
			status = "CREATED";
		}
		
		if(nextAttemptAt==null) {
			nextAttemptAt = LocalDateTime.now();
		}
		
	}

}
