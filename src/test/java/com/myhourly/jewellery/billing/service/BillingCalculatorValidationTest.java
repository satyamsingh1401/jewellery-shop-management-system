package com.myhourly.jewellery.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillCalculationResponse;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingCalculatorValidationTest {

    private final BillingCalculatorService service = new BillingCalculatorService(new BigDecimal("3"));

    private BillItemRequest item(String weight, String rate, MakingChargeType type, String making) {
        return new BillItemRequest(1L, new BigDecimal(weight), new BigDecimal(rate), type,
                new BigDecimal(making));
    }

    private BillItemRequest validItem() {
        return item("10.000", "6000.00", MakingChargeType.PER_GRAM, "500.00");
    }

    private BillCalculationRequest request(BillItemRequest item, DiscountType type, String discount) {
        return new BillCalculationRequest(List.of(item), type,
                discount == null ? null : new BigDecimal(discount));
    }

    private void assertRejected(BillCalculationRequest request, String messageFragment) {
        BusinessValidationException ex =
                assertThrows(BusinessValidationException.class, () -> service.calculate(request));
        assertTrue(ex.getMessage().contains(messageFragment), "unexpected message: " + ex.getMessage());
    }

    // ---------- invalid requests ----------

    @Test
    void rejectsZeroNetWeight() {
        assertRejected(request(item("0", "6000.00", MakingChargeType.PER_GRAM, "500.00"),
                DiscountType.NONE, null), "netWeight");
    }

    @Test
    void rejectsNegativeNetWeight() {
        assertRejected(request(item("-5", "6000.00", MakingChargeType.PER_GRAM, "500.00"),
                DiscountType.NONE, null), "netWeight");
    }

    @Test
    void rejectsNegativeRatePerGram() {
        assertRejected(request(item("10", "-1", MakingChargeType.PER_GRAM, "500.00"),
                DiscountType.NONE, null), "ratePerGram");
    }

    @Test
    void rejectsNegativeMakingCharge() {
        assertRejected(request(item("10", "6000.00", MakingChargeType.FIXED, "-10"),
                DiscountType.NONE, null), "makingChargeValue must not be negative");
    }

    @Test
    void rejectsMissingMakingChargeType() {
        BillItemRequest noType = new BillItemRequest(1L, new BigDecimal("1"), new BigDecimal("1"),
                null, BigDecimal.ZERO);
        assertRejected(request(noType, DiscountType.NONE, null), "makingChargeType");
    }

    @Test
    void rejectsPercentageMakingChargeOverLimit() {
        assertRejected(request(item("10", "6000.00", MakingChargeType.PERCENTAGE, "150"),
                DiscountType.NONE, null), "percentage makingChargeValue must not exceed");
    }

    @Test
    void rejectsNegativeDiscount() {
        assertRejected(request(validItem(), DiscountType.FLAT, "-1"), "discountValue must not be negative");
    }

    @Test
    void rejectsPercentageDiscountOverLimit() {
        assertRejected(request(validItem(), DiscountType.PERCENTAGE, "150"),
                "percentage discountValue must not exceed");
    }

    @Test
    void rejectsFlatDiscountLargerThanSubtotal() {
        assertRejected(request(validItem(), DiscountType.FLAT, "999999"),
                "Discount cannot exceed the bill subtotal");
    }

    @Test
    void rejectsDiscountTypeWithoutValue() {
        assertRejected(request(validItem(), DiscountType.FLAT, null), "discountValue is required");
    }

    @Test
    void rejectsDiscountValueWithoutType() {
        assertRejected(request(validItem(), DiscountType.NONE, "10"), "discountValue requires discountType");
    }

    @Test
    void rejectsEmptyItemList() {
        assertRejected(new BillCalculationRequest(List.of(), DiscountType.NONE, null),
                "At least one item is required");
    }

    @Test
    void rejectsNegativeTaxRateConfiguration() {
        assertThrows(IllegalStateException.class, () -> new BillingCalculatorService(new BigDecimal("-1")));
    }

    @Test
    void rejectsTaxRateAboveHundredConfiguration() {
        assertThrows(IllegalStateException.class, () -> new BillingCalculatorService(new BigDecimal("100.01")));
    }

    // ---------- valid requests ----------

    @Test
    void acceptsPercentageMakingChargeAtTheLimit() {
        BillCalculationResponse response = service.calculate(request(
                item("10.000", "6000.00", MakingChargeType.PERCENTAGE, "100"), DiscountType.NONE, null));

        assertEquals(new BigDecimal("60000.00"), response.items().get(0).makingCharge());
        assertEquals(new BigDecimal("120000.00"), response.subtotal());
    }

    @Test
    void acceptsDiscountEqualToSubtotalWithZeroTaxableAmount() {
        BillCalculationResponse response = service.calculate(
                request(validItem(), DiscountType.FLAT, "65000"));

        assertEquals(new BigDecimal("0.00"), response.taxableAmount());
        assertEquals(new BigDecimal("0.00"), response.tax());
        assertEquals(new BigDecimal("0.00"), response.grandTotal());
    }

    @Test
    void acceptsZeroTaxRate() {
        BillingCalculatorService noTax = new BillingCalculatorService(BigDecimal.ZERO);
        BillCalculationResponse response = noTax.calculate(request(
                item("10.000", "5000.00", MakingChargeType.FIXED, "0"), DiscountType.NONE, null));

        assertEquals(new BigDecimal("0.00"), response.tax());
        assertEquals(new BigDecimal("50000.00"), response.grandTotal());
    }
}