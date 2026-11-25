package com.myhourly.jewellery.customer.repository;

import com.myhourly.jewellery.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

}