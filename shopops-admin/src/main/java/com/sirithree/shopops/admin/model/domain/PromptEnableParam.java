package com.sirithree.shopops.admin.model.domain;

import jakarta.validation.constraints.NotBlank;

public class PromptEnableParam {
    @NotBlank
    private String version;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
