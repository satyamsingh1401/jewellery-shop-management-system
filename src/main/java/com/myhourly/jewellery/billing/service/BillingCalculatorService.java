package com.myhourly.jewellery.billing.service;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillCalculationResponse;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.dto.BillItemResponse;
import com.myhourly.jewellery.billing.enums.DiscountType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingCalculatorService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal taxRatePercent;

    // Tax rate comes from configuration. Confirm the correct rate with your team lead.
    public BillingCalculatorService(@Value("${billing.tax.rate-percent:3}") BigDecimal taxRatePercent) {
        this.taxRatePercent = taxRatePercent;
    }

    public BillCalculationResponse calculate(BillCalculationRequest request) {
        List<BillItemResponse> lines = request.items().stream().map(this::calculateLine).toList();

        BigDecimal subtotal = lines.stream()
                .map(BillItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = calculateDiscount(subtotal, request.discountType(), request.discountValue());
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal tax = money(taxable.multiply(taxRatePercent).divide(HUNDRED));
        BigDecimal grandTotal = taxable.add(tax);

        return new BillCalculationResponse(lines, subtotal, discount, taxable, tax, grandTotal);
    }

    private BillItemResponse calculateLine(BillItemRequest item) {
        BigDecimal metalValue = money(item.netWeight().multiply(item.ratePerGram()));
        BigDecimal makingCharge = switch (item.makingChargeType()) {
            case PER_GRAM -> money(item.netWeight().multiply(item.makingChargeValue()));
            case PERCENTAGE -> money(metalValue.multiply(item.makingChargeValue()).divide(HUNDRED));
            case FIXED -> money(item.makingChargeValue());
        };
        return new BillItemResponse(item.productId(), metalValue, makingCharge, metalValue.add(makingCharge));
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, DiscountType type, BigDecimal value) {
        if (type == null || type == DiscountType.NONE || value == null) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal discount = type == DiscountType.PERCENTAGE
                ? money(subtotal.multiply(value).divide(HUNDRED))
                : money(value);
        if (discount.compareTo(subtotal) > 0) {
            // Later, replace with the shared exception from common/exception
            throw new IllegalArgumentException("Discount cannot exceed the bill subtotal");
        }
        return discount;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
