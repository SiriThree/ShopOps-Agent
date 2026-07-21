package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserPasswordResetParam {
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
