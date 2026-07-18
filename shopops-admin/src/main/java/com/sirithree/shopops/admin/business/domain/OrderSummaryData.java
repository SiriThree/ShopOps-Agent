package com.sirithree.shopops.admin.business.domain;

import java.math.BigDecimal;

public class OrderSummaryData {
    private BigDecimal gmv = BigDecimal.ZERO;
    private Long orderCount = 0L;
    private BigDecimal refundAmount = BigDecimal.ZERO;
    private BigDecimal avgOrderAmount = BigDecimal.ZERO;
    private BigDecimal refundRate = BigDecimal.ZERO;

    public BigDecimal getGmv() {
        return gmv;
    }

    public void setGmv(BigDecimal gmv) {
        this.gmv = gmv == null ? BigDecimal.ZERO : gmv;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount == null ? 0L : orderCount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount == null ? BigDecimal.ZERO : refundAmount;
    }

    public BigDecimal getAvgOrderAmount() {
        return avgOrderAmount;
    }

    public void setAvgOrderAmount(BigDecimal avgOrderAmount) {
        this.avgOrderAmount = avgOrderAmount == null ? BigDecimal.ZERO : avgOrderAmount;
    }

    public BigDecimal getRefundRate() {
        return refundRate;
    }

    public void setRefundRate(BigDecimal refundRate) {
        this.refundRate = refundRate == null ? BigDecimal.ZERO : refundRate;
    }
}
