package com.myhourly.jewellery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockEvent {
	
	private String eventId;
	private Long productId;
	private String productName;
	private int availableQuantity;
	private int threshold;

}
