package com.myhourly.jewellery.reporting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;
import java.util.List;

@ConfigurationProperties(prefix = "reporting")
public record ReportingProperties(
        @DefaultValue({"CONFIRMED", "COMPLETED"}) List<String> saleOrderStatuses,
        @DefaultValue("CANCELLED") String cancelledOrderStatus,
        @DefaultValue("REFUNDED") String refundedPaymentStatus,
        @DefaultValue("366") int maxRangeDays,
        @DefaultValue("10") int defaultTopLimit,
        @DefaultValue("100") int maxTopLimit,
        @DefaultValue("Asia/Kolkata") ZoneId zone
) {

    public ReportingProperties {
        saleOrderStatuses = List.copyOf(saleOrderStatuses);
    }
}
