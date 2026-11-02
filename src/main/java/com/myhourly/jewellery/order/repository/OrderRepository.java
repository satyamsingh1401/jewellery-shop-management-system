package com.myhourly.jewellery.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.myhourly.jewellery.order.entity.Order;
import com.myhourly.jewellery.order.enums.OrderStatus;
import com.myhourly.jewellery.order.enums.PaymentStatus;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByOrderNumber(String orderNumber);
	
	List<Order> findByCustomerId(Long customerId);
	
	List<Order> findByOrderStatus(OrderStatus orderStatus);
	
	List<Order> findByPaymentStatus(PaymentStatus paymentStatus);
	
	List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
	
	boolean existsByOrderNumber(String OrderNumber);
	
	
}
