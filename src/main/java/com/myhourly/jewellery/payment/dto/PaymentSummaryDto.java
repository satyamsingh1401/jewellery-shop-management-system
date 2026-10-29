
package com.myhourly.jewellery.payment.dto;

import java.math.BigDecimal;

public class PaymentSummaryDto {

    private BigDecimal totalAmount;
    private BigDecimal totalRefunded;
    private BigDecimal netAmount;
    private BigDecimal outstandingAmount;

    public PaymentSummaryDto() {
    }

    public PaymentSummaryDto(
            BigDecimal totalAmount,
            BigDecimal totalRefunded,
            BigDecimal netAmount,
            BigDecimal outstandingAmount) {
        this.totalAmount = totalAmount;
        this.totalRefunded = totalRefunded;
        this.netAmount = netAmount;
        this.outstandingAmount = outstandingAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalRefunded() {
        return totalRefunded;
    }

    public void setTotalRefunded(BigDecimal totalRefunded) {
        this.totalRefunded = totalRefunded;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }
}
