package com.myhourly.jewellery.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillCalculationResponse;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingCalculatorServiceTest {

    private final BillingCalculatorService service = new BillingCalculatorService(new BigDecimal("3"));

    @Test
    void calculatesBillWithPerGramMakingChargeAndPercentageDiscount() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), MakingChargeType.PER_GRAM, new BigDecimal("500.00"));
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.PERCENTAGE, new BigDecimal("10"));

        BillCalculationResponse response = service.calculate(request);

        assertEquals(new BigDecimal("65000.00"), response.subtotal());
        assertEquals(new BigDecimal("6500.00"), response.discount());
        assertEquals(new BigDecimal("58500.00"), response.taxableAmount());
        assertEquals(new BigDecimal("1755.00"), response.tax());
        assertEquals(new BigDecimal("60255.00"), response.grandTotal());
    }

    @Test
    void rejectsDiscountLargerThanSubtotal() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("1.000"),
                new BigDecimal("1000.00"), MakingChargeType.FIXED, new BigDecimal("0"));
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.FLAT, new BigDecimal("5000"));

        assertThrows(IllegalArgumentException.class, () -> service.calculate(request));
    }
}