package com.myhourly.jewellery.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationRequest {
	
	@NotBlank
	@Email
	private String recipient;
	
	@NotBlank
	@Size(max = 50)
	private String type;
	
	@NotBlank
	@Size(max = 1000)
	private String message;

}
