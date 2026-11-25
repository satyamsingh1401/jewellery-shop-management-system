package com.myhourly.jewellery.payment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "jms.mysql.database", matches = "jms_payment_verify_[0-9]+")
@ActiveProfiles(value = "mysql-verification", inheritProfiles = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentRepositoryMySqlTest extends PaymentRepositoryTest {
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void requireScratchDatabase() {
        assertEquals(System.getProperty("jms.mysql.database"),
                jdbc.queryForObject("select database()", String.class));
    }
}
