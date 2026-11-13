
package com.myhourly.jewellery.customer.controller;

import com.myhourly.jewellery.customer.dto.CustomerRequestDto;
import com.myhourly.jewellery.customer.dto.CustomerResponseDto;
import com.myhourly.jewellery.customer.service.CustomerService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // 1. Create Customer
    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CustomerRequestDto request) {

        CustomerResponseDto response =
                customerService.createCustomer(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. Get Customer By ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(
            @PathVariable Long id) {

        CustomerResponseDto response =
                customerService.getCustomerById(id);

        return ResponseEntity.ok(response);
    }

    // 3. Get All Customers
    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {

        List<CustomerResponseDto> customers =
                customerService.getAllCustomers();

        return ResponseEntity.ok(customers);
    }

    // 4. Update Customer
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDto request) {

        CustomerResponseDto response =
                customerService.updateCustomer(id, request);

        return ResponseEntity.ok(response);
    }

    // 5. Delete Customer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id) {

        customerService.deleteCustomer(id);

        return ResponseEntity.noContent().build();
    }
}
