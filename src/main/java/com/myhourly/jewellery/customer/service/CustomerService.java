package com.myhourly.jewellery.customer.service;

import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.customer.dto.CustomerRequestDto;
import com.myhourly.jewellery.customer.dto.CustomerResponseDto;
import com.myhourly.jewellery.customer.entity.Customer;
import com.myhourly.jewellery.customer.mapper.CustomerMapper;
import com.myhourly.jewellery.customer.repository.CustomerRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper) {

        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    // 1. Create Customer
    public CustomerResponseDto createCustomer(
            CustomerRequestDto request) {

        Customer customer = customerMapper.toEntity(request);

        Customer savedCustomer = customerRepository.save(customer);

        return customerMapper.toResponse(savedCustomer);
    }

    // 2. Get Customer By ID
    public CustomerResponseDto getCustomerById(Long id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id));

        return customerMapper.toResponse(customer);
    }

    // 3. Get All Customers
    public List<CustomerResponseDto> getAllCustomers() {

        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    // 4. Update Customer
    public CustomerResponseDto updateCustomer(
            Long id,
            CustomerRequestDto request) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id));

        customerMapper.updateEntity(customer, request);

        Customer updatedCustomer = customerRepository.save(customer);

        return customerMapper.toResponse(updatedCustomer);
    }

    //5. Delete Customer
    public void deleteCustomer(Long id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id));

        customerRepository.delete(customer);
    }


}