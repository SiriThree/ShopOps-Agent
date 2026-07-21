package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ShopUpsertParam {
    @NotBlank
    @Size(max = 64)
    private String shopNo;
    @NotBlank
    @Size(max = 128)
    private String shopName;
    @NotBlank
    @Size(max = 32)
    private String platformType;
    @NotNull
    private Long ownerId;
    @NotBlank
    @Size(max = 32)
    private String status;

    public String getShopNo() { return shopNo; }
    public void setShopNo(String shopNo) { this.shopNo = shopNo; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getPlatformType() { return platformType; }
    public void setPlatformType(String platformType) { this.platformType = platformType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
