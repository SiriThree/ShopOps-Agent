package com.sirithree.shopops.admin.tool.domain;

public class ToolGovernanceException extends RuntimeException {
    private final String errorCode;

    public ToolGovernanceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
