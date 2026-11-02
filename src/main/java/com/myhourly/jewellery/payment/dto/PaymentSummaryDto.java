
package com.myhourly.jewellery.payment.dto;

import java.math.BigDecimal;

public class PaymentSummaryDto {

    private Long billId;
    private BigDecimal billTotal;
    private BigDecimal totalAmount;
    private BigDecimal totalRefunded;
    private BigDecimal netAmount;
    private BigDecimal outstandingAmount;

    public PaymentSummaryDto() {
    }

    public PaymentSummaryDto(
            Long billId,
            BigDecimal billTotal,
            
            BigDecimal totalAmount,
            BigDecimal totalRefunded,
            BigDecimal netAmount,
            BigDecimal outstandingAmount) {
        this.billId = billId;
        this.billTotal = billTotal;
        this.totalAmount = totalAmount;
        this.totalRefunded = totalRefunded;
        this.netAmount = netAmount;
        this.outstandingAmount = outstandingAmount;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public BigDecimal getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(BigDecimal billTotal) {
        this.billTotal = billTotal;
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
