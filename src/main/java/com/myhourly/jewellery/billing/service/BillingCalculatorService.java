package com.myhourly.jewellery.billing.service;


import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillCalculationResponse;
import com.myhourly.jewellery.billing.dto.BillItemRequest;
import com.myhourly.jewellery.billing.dto.BillItemResponse;
import com.myhourly.jewellery.billing.enums.DiscountType;
import com.myhourly.jewellery.billing.enums.MakingChargeType;
import com.myhourly.jewellery.common.exception.BusinessValidationException; // REPLACE with the real shared class
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingCalculatorService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    // Assumed limits: confirm with the team lead.
    private static final BigDecimal MAX_MAKING_CHARGE_PERCENT = HUNDRED;
    private static final BigDecimal MAX_DISCOUNT_PERCENT = HUNDRED;

    private final BigDecimal taxRatePercent;

    public BillingCalculatorService(@Value("${billing.tax.rate-percent:3}") BigDecimal taxRatePercent) {
        // A bad tax rate is a configuration mistake, not a client error, so the app refuses to start.
        if (taxRatePercent == null || taxRatePercent.signum() < 0 || taxRatePercent.compareTo(HUNDRED) > 0) {
            throw new IllegalStateException(
                    "billing.tax.rate-percent must be between 0 and 100 but was " + taxRatePercent);
        }
        this.taxRatePercent = taxRatePercent;
    }

    public BillCalculationResponse calculate(BillCalculationRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw invalid("At least one item is required");
        }
        validateDiscount(request.discountType(), request.discountValue());

        List<BillItemResponse> lines = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            BillItemRequest item = request.items().get(i);
            validateItem(item, i);
            lines.add(calculateLine(item));
        }

        BigDecimal subtotal = lines.stream()
                .map(BillItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = calculateDiscount(subtotal, request.discountType(), request.discountValue());
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal tax = money(taxable.multiply(taxRatePercent).divide(HUNDRED));
        BigDecimal grandTotal = taxable.add(tax);

        return new BillCalculationResponse(lines, subtotal, discount, taxable, tax, grandTotal);
    }

    private void validateItem(BillItemRequest item, int position) {
        String where = "items[" + position + "]: ";
        if (item == null) {
            throw invalid(where + "item is required");
        }
        if (item.netWeight() == null || item.netWeight().signum() <= 0) {
            throw invalid(where + "netWeight must be greater than zero");
        }
        if (item.ratePerGram() == null || item.ratePerGram().signum() <= 0) {
            throw invalid(where + "ratePerGram must be greater than zero");
        }
        if (item.makingChargeType() == null) {
            throw invalid(where + "makingChargeType is required");
        }
        if (item.makingChargeValue() == null || item.makingChargeValue().signum() < 0) {
            throw invalid(where + "makingChargeValue must not be negative");
        }
        if (item.makingChargeType() == MakingChargeType.PERCENTAGE
                && item.makingChargeValue().compareTo(MAX_MAKING_CHARGE_PERCENT) > 0) {
            throw invalid(where + "percentage makingChargeValue must not exceed " + MAX_MAKING_CHARGE_PERCENT);
        }
    }

    private void validateDiscount(DiscountType type, BigDecimal value) {
        if (type == null || type == DiscountType.NONE) {
            if (value != null && value.signum() != 0) {
                throw invalid("discountValue requires discountType PERCENTAGE or FLAT");
            }
            return;
        }
        if (value == null) {
            throw invalid("discountValue is required for discountType " + type);
        }
        if (value.signum() < 0) {
            throw invalid("discountValue must not be negative");
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(MAX_DISCOUNT_PERCENT) > 0) {
            throw invalid("percentage discountValue must not exceed " + MAX_DISCOUNT_PERCENT);
        }
    }

       private BillItemResponse calculateLine(BillItemRequest item) {

        if (item.netWeight() == null
                || item.netWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Net weight must be greater than zero");
        }

        if (item.ratePerGram() == null
                || item.ratePerGram().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Rate per gram cannot be negative or null");
        }

        if (item.makingChargeValue() == null
                || item.makingChargeValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Making charge value cannot be negative or null");
        }

        BigDecimal metalValue = money(
                item.netWeight().multiply(item.ratePerGram()));

        BigDecimal makingCharge = switch (item.makingChargeType()) {
            case PER_GRAM -> money(
                    item.netWeight().multiply(item.makingChargeValue()));

            case PERCENTAGE -> money(
                    metalValue.multiply(item.makingChargeValue())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP));

            case FIXED -> money(item.makingChargeValue());
        };

        BigDecimal lineTotal = money(metalValue.add(makingCharge));

        return new BillItemResponse(
                item.productId(),
                metalValue,
                makingCharge,
                lineTotal);
    }

       private BigDecimal calculateDiscount(
            BigDecimal subtotal,
            DiscountType type,
            BigDecimal value) {

        if (type == null || type == DiscountType.NONE) {
            return money(BigDecimal.ZERO);
        }

        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Discount value cannot be null or negative");
        }

        BigDecimal discount = switch (type) {
            case PERCENTAGE -> {
                if (value.compareTo(HUNDRED) > 0) {
                    throw new BusinessValidationException(
                            "Percentage discount cannot exceed 100%");
                }

                yield money(
                        subtotal.multiply(value)
                                .divide(HUNDRED, 2, RoundingMode.HALF_UP));
            }

            case FLAT -> money(value);

            case NONE -> money(BigDecimal.ZERO);
        };

        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessValidationException(
                    "Discount cannot exceed the bill subtotal");
        }

        return discount;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // The one place that creates the shared exception. Change only this line if its constructor differs.
    private static BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
