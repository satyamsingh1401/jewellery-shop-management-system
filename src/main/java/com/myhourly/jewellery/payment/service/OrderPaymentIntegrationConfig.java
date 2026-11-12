package com.myhourly.jewellery.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Safe default until the Order module provides its own
 * OrderPaymentStatusUpdater bean. When that bean exists, this one is skipped.
 */
@Configuration
public class OrderPaymentIntegrationConfig {

    private static final Logger log =
            LoggerFactory.getLogger(OrderPaymentIntegrationConfig.class);

    @Bean
    @ConditionalOnMissingBean(OrderPaymentStatusUpdater.class)
    OrderPaymentStatusUpdater noOpOrderPaymentStatusUpdater() {
        return (orderId, status) -> log.warn(
                "Order module not connected: payment status {} for order {} was not sent",
                status, orderId);
    }
}