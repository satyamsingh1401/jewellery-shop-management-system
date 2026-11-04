package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.reporting.config.ReportingClock;
import com.myhourly.jewellery.reporting.config.ReportingProperties;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class ReportRequestValidator {

    private final ReportingProperties properties;
    private final ReportingClock clock;

    public ReportRequestValidator(ReportingProperties properties, ReportingClock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ReportPeriod day(LocalDate date) {
        requireDate(date, "date");
        return ReportPeriod.ofDay(date);
    }

    public ReportPeriod range(LocalDate from, LocalDate to) {
        requireDate(from, "from");
        requireDate(to, "to");
        if (from.isAfter(to)) {
            throw new BusinessValidationException("'from' must be on or before 'to'");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > properties.maxRangeDays()) {
            throw new BusinessValidationException(
                    "Date range must not exceed " + properties.maxRangeDays() + " days");
        }
        return new ReportPeriod(from, to);
    }

    public int limit(Integer requested) {
        if (requested == null) {
            return properties.defaultTopLimit();
        }
        if (requested < 1 || requested > properties.maxTopLimit()) {
            throw new BusinessValidationException(
                    "'limit' must be between 1 and " + properties.maxTopLimit());
        }
        return requested;
    }

    private void requireDate(LocalDate date, String name) {
        if (date == null) {
            throw new BusinessValidationException("'" + name + "' is required");
        }
        if (date.isAfter(clock.today())) {
            throw new BusinessValidationException("'" + name + "' must not be in the future");
        }
    }
}
