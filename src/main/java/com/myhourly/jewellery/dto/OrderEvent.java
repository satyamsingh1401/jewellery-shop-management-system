package com.myhourly.jewellery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
	
	private String eventId;
	private Long orderId;
	private String customerEmail;
	private String status;
	

}
