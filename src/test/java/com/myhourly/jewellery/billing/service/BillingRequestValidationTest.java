package com.myhourly.jewellery.billing.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BillingRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalidFields(Object target) {
        return validator.validate(target).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private BillItemRequest item(String weight, String rate, String making) {
        return new BillItemRequest(1L, new BigDecimal(weight), new BigDecimal(rate),
                MakingChargeType.PER_GRAM, new BigDecimal(making));
    }

    @Test
    void validItemHasNoViolations() {
        assertTrue(invalidFields(item("10.000", "6000.00", "500.00")).isEmpty());
    }

    @Test
    void zeroAndNegativeNetWeightAreRejected() {
        assertTrue(invalidFields(item("0", "6000.00", "500.00")).contains("netWeight"));
        assertTrue(invalidFields(item("-5", "6000.00", "500.00")).contains("netWeight"));
    }

    @Test
    void negativeRatePerGramIsRejected() {
        assertTrue(invalidFields(item("10", "-1", "500.00")).contains("ratePerGram"));
    }

    @Test
    void negativeMakingChargeIsRejected() {
        assertTrue(invalidFields(item("10", "6000.00", "-10")).contains("makingChargeValue"));
    }

    @Test
    void tooManyDecimalPlacesAreRejected() {
        assertTrue(invalidFields(item("1.2345", "6000.00", "500.00")).contains("netWeight"));
    }

    @Test
    void missingFieldsAreRejected() {
        Set<String> fields = invalidFields(new BillItemRequest(1L, null, null, null, null));
        assertTrue(fields.containsAll(Set.of("netWeight", "ratePerGram", "makingChargeType", "makingChargeValue")));
    }

    @Test
    void negativeDiscountValueIsRejected() {
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item("10", "6000.00", "500.00")), DiscountType.FLAT, new BigDecimal("-1"));
        assertTrue(invalidFields(request).contains("discountValue"));
    }

    @Test
    void emptyItemListIsRejected() {
        assertTrue(invalidFields(new BillCalculationRequest(List.of(), DiscountType.NONE, null))
                .contains("items"));
    }

    @Test
    void invalidNestedItemIsReportedWithItsPosition() {
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item("-5", "6000.00", "500.00")), DiscountType.NONE, null);
        assertTrue(invalidFields(request).contains("items[0].netWeight"));
    }
}