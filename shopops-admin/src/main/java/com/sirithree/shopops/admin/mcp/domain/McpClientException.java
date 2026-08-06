package com.sirithree.shopops.admin.mcp.domain;

public class McpClientException extends RuntimeException {
    private final String errorCode;

    public McpClientException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public McpClientException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
