package com.myhourly.jewellery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JewelleryShopManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(JewelleryShopManagementSystemApplication.class, args);
	}

}
