package com.sirithree.shopops.admin.persistence.model;

import java.time.LocalDateTime;

public class ConnectorSyncJob {
    private Long id;
    private Long tenantId;
    private Long shopId;
    private String connectorCode;
    private String status;
    private Integer attempt;
    private Integer maxAttempts;
    private String triggerType;
    private Long createdBy;
    private String requestId;
    private String message;
    private String detailJson;
    private String cursorValue;
    private String checkpointJson;
    private String errorType;
    private LocalDateTime nextRetryAt;
    private String workerId;
    private LocalDateTime leaseExpireAt;
    private LocalDateTime heartbeatAt;
    private LocalDateTime cancelRequestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getConnectorCode() { return connectorCode; }
    public void setConnectorCode(String connectorCode) { this.connectorCode = connectorCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAttempt() { return attempt; }
    public void setAttempt(Integer attempt) { this.attempt = attempt; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
    public String getCursorValue(){return cursorValue;} public void setCursorValue(String v){cursorValue=v;}
    public String getCheckpointJson(){return checkpointJson;} public void setCheckpointJson(String v){checkpointJson=v;}
    public String getErrorType(){return errorType;} public void setErrorType(String v){errorType=v;}
    public LocalDateTime getNextRetryAt(){return nextRetryAt;} public void setNextRetryAt(LocalDateTime v){nextRetryAt=v;}
    public String getWorkerId(){return workerId;} public void setWorkerId(String v){workerId=v;}
    public LocalDateTime getLeaseExpireAt(){return leaseExpireAt;} public void setLeaseExpireAt(LocalDateTime v){leaseExpireAt=v;}
    public LocalDateTime getHeartbeatAt(){return heartbeatAt;} public void setHeartbeatAt(LocalDateTime v){heartbeatAt=v;}
    public LocalDateTime getCancelRequestedAt(){return cancelRequestedAt;} public void setCancelRequestedAt(LocalDateTime v){cancelRequestedAt=v;}
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
