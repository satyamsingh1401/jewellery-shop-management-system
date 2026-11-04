package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.reporting.config.ReportingClock;
import com.myhourly.jewellery.reporting.config.ReportingProperties;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReportRequestValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

    @Mock
    private ReportingClock clock;

    private ReportRequestValidator validator;

    @BeforeEach
    void setUp() {
        lenient().when(clock.today()).thenReturn(TODAY);
        ReportingProperties properties = new ReportingProperties(
                List.of("CONFIRMED", "COMPLETED"), "CANCELLED", "REFUNDED", 366, 10, 100, ZoneId.of("Asia/Kolkata"));
        validator = new ReportRequestValidator(properties, clock);
    }

    @Test
    void dayAcceptsToday() {
        ReportPeriod period = validator.day(TODAY);

        assertThat(period.from()).isEqualTo(TODAY);
        assertThat(period.to()).isEqualTo(TODAY);
    }

    @Test
    void dayAcceptsPastDate() {
        assertThat(validator.day(TODAY.minusYears(3)).from()).isEqualTo(TODAY.minusYears(3));
    }

    @Test
    void dayRejectsNull() {
        assertThatThrownBy(() -> validator.day(null))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'date' is required");
    }

    @Test
    void dayRejectsFutureDate() {
        assertThatThrownBy(() -> validator.day(TODAY.plusDays(1)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'date' must not be in the future");
    }

    @Test
    void rangeAcceptsValidRange() {
        ReportPeriod period = validator.range(TODAY.minusDays(6), TODAY);

        assertThat(period.from()).isEqualTo(TODAY.minusDays(6));
        assertThat(period.to()).isEqualTo(TODAY);
    }

    @Test
    void rangeAcceptsSameDay() {
        ReportPeriod period = validator.range(TODAY, TODAY);

        assertThat(period.days()).containsExactly(TODAY);
    }

    @Test
    void rangeRejectsNullFrom() {
        assertThatThrownBy(() -> validator.range(null, TODAY))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'from' is required");
    }

    @Test
    void rangeRejectsNullTo() {
        assertThatThrownBy(() -> validator.range(TODAY, null))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'to' is required");
    }

    @Test
    void rangeRejectsFutureFrom() {
        assertThatThrownBy(() -> validator.range(TODAY.plusDays(1), TODAY.plusDays(2)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'from' must not be in the future");
    }

    @Test
    void rangeRejectsFutureTo() {
        assertThatThrownBy(() -> validator.range(TODAY.minusDays(1), TODAY.plusDays(1)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'to' must not be in the future");
    }

    @Test
    void rangeRejectsFromAfterTo() {
        assertThatThrownBy(() -> validator.range(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("'from' must be on or before 'to'");
    }

    @Test
    void rangeAcceptsExactlyMaxDaysInclusive() {
        LocalDate from = TODAY.minusDays(365);

        ReportPeriod period = validator.range(from, TODAY);

        assertThat(period.days()).hasSize(366);
    }

    @Test
    void rangeRejectsOneDayOverMax() {
        assertThatThrownBy(() -> validator.range(TODAY.minusDays(366), TODAY))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("366 days");
    }

    @Test
    void limitDefaultsWhenNull() {
        assertThat(validator.limit(null)).isEqualTo(10);
        verifyNoInteractions(clock);
    }

    @Test
    void limitAcceptsBounds() {
        assertThat(validator.limit(1)).isEqualTo(1);
        assertThat(validator.limit(100)).isEqualTo(100);
    }

    @Test
    void limitRejectsZero() {
        assertThatThrownBy(() -> validator.limit(0))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("between 1 and 100");
    }

    @Test
    void limitRejectsNegative() {
        assertThatThrownBy(() -> validator.limit(-5))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void limitRejectsAboveMax() {
        assertThatThrownBy(() -> validator.limit(101))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("between 1 and 100");
    }
}
