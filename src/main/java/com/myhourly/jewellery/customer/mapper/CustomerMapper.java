
package com.myhourly.jewellery.customer.mapper;

import com.myhourly.jewellery.customer.dto.CustomerRequestDto;
import com.myhourly.jewellery.customer.dto.CustomerResponseDto;
import com.myhourly.jewellery.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequestDto dto) {
        Customer customer = new Customer();

        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());

        return customer;
    }

    public void updateEntity(Customer customer, CustomerRequestDto dto) {
        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
    }

    public CustomerResponseDto toResponse(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();

        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setPhone(customer.getPhone());
        dto.setEmail(customer.getEmail());
        dto.setAddress(customer.getAddress());
        dto.setCreatedAt(customer.getCreatedAt());

        return dto;
    }
}
