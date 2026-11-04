package com.myhourly.jewellery.reporting.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ReportPeriod(LocalDate from, LocalDate to) {

    public ReportPeriod {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    public static ReportPeriod ofDay(LocalDate day) {
        return new ReportPeriod(day, day);
    }

    public LocalDateTime startInclusive() {
        return from.atStartOfDay();
    }

    public LocalDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay();
    }

    public List<LocalDate> days() {
        return from.datesUntil(to.plusDays(1)).toList();
    }
}
