package com.myhourly.jewellery.reporting.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReportingClock {

    private final ReportingProperties properties;

    public ReportingClock(ReportingProperties properties) {
        this.properties = properties;
    }

    public LocalDate today() {
        return LocalDate.now(properties.zone());
    }
}
