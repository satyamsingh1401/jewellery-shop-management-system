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
        @Test
    void matchesDocumentExampleSubtotal50000WithThreePercentTax() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("5000.00"), MakingChargeType.FIXED, BigDecimal.ZERO);
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.NONE, null);

        BillCalculationResponse response = service.calculate(request);

        assertEquals(new BigDecimal("50000.00"), response.subtotal());
        assertEquals(new BigDecimal("1500.00"), response.tax());
        assertEquals(new BigDecimal("51500.00"), response.grandTotal());
    }
        @Test
    void calculatesPercentageMakingCharge() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), MakingChargeType.PERCENTAGE, new BigDecimal("8"));
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.NONE, null);

        BillCalculationResponse response = service.calculate(request);

        assertEquals(new BigDecimal("4800.00"), response.items().get(0).makingCharge());
        assertEquals(new BigDecimal("64800.00"), response.subtotal());
        assertEquals(new BigDecimal("1944.00"), response.tax());
        assertEquals(new BigDecimal("66744.00"), response.grandTotal());
    }

    @Test
    void calculatesTwoItemsInOneBill() {
        BillItemRequest first = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("6000.00"), MakingChargeType.PER_GRAM, new BigDecimal("500.00"));
        BillItemRequest second = new BillItemRequest(2L, new BigDecimal("2.000"),
                new BigDecimal("5500.00"), MakingChargeType.FIXED, new BigDecimal("1000.00"));
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(first, second), DiscountType.NONE, null);

        BillCalculationResponse response = service.calculate(request);

        assertEquals(2, response.items().size());
        assertEquals(new BigDecimal("77000.00"), response.subtotal());
        assertEquals(new BigDecimal("2310.00"), response.tax());
        assertEquals(new BigDecimal("79310.00"), response.grandTotal());
    }

    @Test
    void appliesFlatDiscountBeforeTax() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("5.000"),
                new BigDecimal("6000.00"), MakingChargeType.FIXED, new BigDecimal("2000.00"));
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.FLAT, new BigDecimal("800"));

        BillCalculationResponse response = service.calculate(request);

        assertEquals(new BigDecimal("32000.00"), response.subtotal());
        assertEquals(new BigDecimal("800.00"), response.discount());
        assertEquals(new BigDecimal("31200.00"), response.taxableAmount());
        assertEquals(new BigDecimal("936.00"), response.tax());
        assertEquals(new BigDecimal("32136.00"), response.grandTotal());
    }

    @Test
    void rejectsPercentageDiscountOverHundred() {
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("1.000"),
                new BigDecimal("1000.00"), MakingChargeType.FIXED, BigDecimal.ZERO);
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.PERCENTAGE, new BigDecimal("150"));

        assertThrows(IllegalArgumentException.class, () -> service.calculate(request));
    }

    @Test
    void usesConfiguredTaxRate() {
        BillingCalculatorService fivePercent = new BillingCalculatorService(new BigDecimal("5"));
        BillItemRequest item = new BillItemRequest(1L, new BigDecimal("10.000"),
                new BigDecimal("5000.00"), MakingChargeType.FIXED, BigDecimal.ZERO);
        BillCalculationRequest request = new BillCalculationRequest(
                List.of(item), DiscountType.NONE, null);

        BillCalculationResponse response = fivePercent.calculate(request);

        assertEquals(new BigDecimal("2500.00"), response.tax());
        assertEquals(new BigDecimal("52500.00"), response.grandTotal());
    }
}