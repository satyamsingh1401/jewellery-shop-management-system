package com.myhourly.jewellery.reporting.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPeriodTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 15);

    @Test
    void ofDayCoversSingleDay() {
        ReportPeriod period = ReportPeriod.ofDay(DAY);

        assertThat(period.from()).isEqualTo(DAY);
        assertThat(period.to()).isEqualTo(DAY);
        assertThat(period.days()).containsExactly(DAY);
    }

    @Test
    void daysAreInclusiveOfBothEnds() {
        ReportPeriod period = new ReportPeriod(DAY, DAY.plusDays(2));

        assertThat(period.days()).containsExactly(DAY, DAY.plusDays(1), DAY.plusDays(2));
    }

    @Test
    void daysSpanMonthBoundary() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 2));

        assertThat(period.days()).containsExactly(
                LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
    }

    @Test
    void startInclusiveIsStartOfFromDay() {
        ReportPeriod period = new ReportPeriod(DAY, DAY.plusDays(1));

        assertThat(period.startInclusive()).isEqualTo(LocalDateTime.of(2026, 3, 15, 0, 0));
    }

    @Test
    void endExclusiveIsStartOfDayAfterTo() {
        ReportPeriod period = new ReportPeriod(DAY, DAY.plusDays(1));

        assertThat(period.endExclusive()).isEqualTo(LocalDateTime.of(2026, 3, 17, 0, 0));
    }

    @Test
    void endExclusiveCrossesYearBoundary() {
        ReportPeriod period = ReportPeriod.ofDay(LocalDate.of(2025, 12, 31));

        assertThat(period.endExclusive()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> new ReportPeriod(DAY.plusDays(1), DAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullBounds() {
        assertThatThrownBy(() -> new ReportPeriod(null, DAY)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReportPeriod(DAY, null)).isInstanceOf(NullPointerException.class);
    }
}
