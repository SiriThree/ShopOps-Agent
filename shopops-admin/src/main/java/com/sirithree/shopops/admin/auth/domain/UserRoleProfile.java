package com.sirithree.shopops.admin.auth.domain;

import java.util.List;

public class UserRoleProfile {
    private final String username;
    private final List<String> roles;

    public UserRoleProfile(String username, List<String> roles) {
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}
