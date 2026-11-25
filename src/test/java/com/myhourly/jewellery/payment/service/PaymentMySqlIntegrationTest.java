package com.myhourly.jewellery.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Run the same committed-data and JWT scenarios on an explicitly named scratch
 * MySQL database. Normal builds keep using the in-memory tests.
 */
@EnabledIfSystemProperty(named = "jms.mysql.database", matches = "jms_payment_verify_[0-9]+")
@ActiveProfiles(value = "mysql-verification", inheritProfiles = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentMySqlIntegrationTest extends PaymentOrderBillingIntegrationTest {
    @BeforeEach
    void requireScratchDatabase() {
        assertEquals(System.getProperty("jms.mysql.database"),
                jdbc.queryForObject("select database()", String.class));
    }
}
