
package com.myhourly.jewellery.customer.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerResponseDto {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt;
}
