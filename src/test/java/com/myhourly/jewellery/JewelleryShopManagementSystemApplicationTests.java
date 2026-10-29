
package com.myhourly.jewellery;

import com.myhourly.jewellery.payment.Payment;
import com.myhourly.jewellery.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JewelleryShopManagementSystemApplicationTests {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void contextLoads() {
        assertNotNull(paymentRepository);
    }

    @Test
    void paymentRepositoryBeanLoads() {
        assertNotNull(paymentRepository);
    }

    @Test
    void findPaymentByIdReturnsOptional() {
        Optional<Payment> result = paymentRepository.findById(-1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
