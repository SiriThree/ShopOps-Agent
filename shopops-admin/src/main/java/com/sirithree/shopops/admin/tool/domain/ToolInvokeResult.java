package com.sirithree.shopops.admin.tool.domain;

public class ToolInvokeResult {
    private Boolean success;
    private String status;
    private Object data;
    private Long toolCallLogId;
    private String errorCode;
    private String errorMessage;

    public static ToolInvokeResult success(Object data, Long toolCallLogId) {
        ToolInvokeResult result = new ToolInvokeResult();
        result.setSuccess(true);
        result.setStatus("SUCCESS");
        result.setData(data);
        result.setToolCallLogId(toolCallLogId);
        return result;
    }

    public static ToolInvokeResult failed(String errorCode, String errorMessage, Long toolCallLogId) {
        ToolInvokeResult result = new ToolInvokeResult();
        result.setSuccess(false);
        result.setStatus("FAILED");
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setToolCallLogId(toolCallLogId);
        return result;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Long getToolCallLogId() {
        return toolCallLogId;
    }

    public void setToolCallLogId(Long toolCallLogId) {
        this.toolCallLogId = toolCallLogId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
