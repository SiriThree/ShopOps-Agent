package com.sirithree.shopops.admin.auth.domain;

import java.util.Locale;

public enum AuthRole {
    VIEWER(1),
    OPERATOR(2),
    ADMIN(3);

    private final int level;

    AuthRole(int level) {
        this.level = level;
    }

    public boolean includes(AuthRole requiredRole) {
        return level >= requiredRole.level;
    }

    public static AuthRole parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AuthRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
