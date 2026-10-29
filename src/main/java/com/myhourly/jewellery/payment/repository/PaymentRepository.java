
package com.myhourly.jewellery.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.myhourly.jewellery.payment.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
