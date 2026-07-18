package com.sirithree.shopops.admin.business.domain;

import java.math.BigDecimal;

public class ProductCandidateRow {
    private Long productId;
    private String productName;
    private String categoryName;
    private String title;
    private Integer stock;
    private Long salesQuantity;
    private BigDecimal salesAmount;
    private Long negativeCount;
    private BigDecimal avgStar;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Long getSalesQuantity() {
        return salesQuantity;
    }

    public void setSalesQuantity(Long salesQuantity) {
        this.salesQuantity = salesQuantity;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Long getNegativeCount() {
        return negativeCount;
    }

    public void setNegativeCount(Long negativeCount) {
        this.negativeCount = negativeCount;
    }

    public BigDecimal getAvgStar() {
        return avgStar;
    }

    public void setAvgStar(BigDecimal avgStar) {
        this.avgStar = avgStar;
    }
}
