package com.sirithree.shopops.admin.auth.domain;

import java.time.Instant;
import java.util.List;

public class LoginResult {
    private String tokenType;
    private String accessToken;
    private Instant expiresAt;
    private CurrentUserDto user;

    public LoginResult(String tokenType, String accessToken, Instant expiresAt, CurrentUserDto user) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public CurrentUserDto getUser() { return user; }
    public void setUser(CurrentUserDto user) { this.user = user; }

    public static LoginResult of(String accessToken, Instant expiresAt, Long tenantId, Long shopId,
                                 Long userId, String username, List<String> roles) {
        CurrentUserDto user = new CurrentUserDto();
        user.setTenantId(tenantId);
        user.setShopId(shopId);
        user.setUserId(userId);
        user.setUsername(username);
        user.setRoles(roles);
        user.setAuthType("BEARER");
        user.setAuthenticated(true);
        return new LoginResult("Bearer", accessToken, expiresAt, user);
    }
}
