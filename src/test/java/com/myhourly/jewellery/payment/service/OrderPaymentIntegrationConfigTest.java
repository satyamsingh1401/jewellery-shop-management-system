package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.payment.enums.OrderPaymentStatus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderPaymentIntegrationConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(OrderPaymentIntegrationConfig.class);

    @Test
    void shouldUseSafeDefaultWhenOrderModuleIsNotConnected() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(OrderPaymentStatusUpdater.class);

            OrderPaymentStatusUpdater updater =
                    context.getBean(OrderPaymentStatusUpdater.class);
            assertDoesNotThrow(() -> updater.updatePaymentStatus(
                    1L, OrderPaymentStatus.PAID));
        });
    }

    @Test
    void shouldBackOffWhenOrderModuleProvidesItsOwnBean() {
        OrderPaymentStatusUpdater orderModuleBean = (orderId, status) -> { };

        runner.withBean("orderModuleUpdater",
                        OrderPaymentStatusUpdater.class,
                        () -> orderModuleBean)
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(OrderPaymentStatusUpdater.class);
                    assertThat(context.getBean(OrderPaymentStatusUpdater.class))
                            .isSameAs(orderModuleBean);
                });
    }
}