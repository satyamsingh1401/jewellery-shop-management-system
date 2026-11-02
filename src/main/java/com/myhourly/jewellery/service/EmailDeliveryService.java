package com.myhourly.jewellery.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailDeliveryService {
	
	private final JavaMailSender javaMailSender;
	
	public void send(String recipient, String type, String message) {
		
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setTo(recipient);
		mail.setSubject("Jewellery Shop - " +type);
		mail.setText(message);
		
		javaMailSender.send(mail);
	}

}
