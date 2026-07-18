package com.sirithree.shopops.admin.common.context;

public class RequestContext {
    private final Long tenantId;
    private final Long shopId;
    private final Long userId;
    private final String requestId;

    public RequestContext(Long tenantId, Long shopId, Long userId, String requestId) {
        this.tenantId = tenantId;
        this.shopId = shopId;
        this.userId = userId;
        this.requestId = requestId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getShopId() {
        return shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }
}
