package com.sirithree.shopops.admin.auth.domain;

public class LogoutResult {
    private String tokenId;
    private String status;

    public LogoutResult(String tokenId, String status) {
        this.tokenId = tokenId;
        this.status = status;
    }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
