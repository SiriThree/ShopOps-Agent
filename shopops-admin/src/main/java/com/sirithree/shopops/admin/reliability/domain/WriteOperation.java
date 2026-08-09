package com.sirithree.shopops.admin.reliability.domain;

import java.time.LocalDateTime;

public class WriteOperation {
    private Long id; private Long tenantId; private Long shopId; private Long userId; private Long taskId;
    private String traceId; private String toolCode; private String businessObjectId; private String operationRequestId;
    private String idempotencyKey; private String inputHash; private Long approvalId; private String status;
    private String externalReference; private String resultJson; private String lastErrorCode; private String lastErrorMessage;
    private String retryAction; private Integer recoveryAttemptCount; private LocalDateTime lastRecoveryAt; private Integer version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    private boolean freshExecution;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public Long getShopId(){return shopId;} public void setShopId(Long v){shopId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;} public String getTraceId(){return traceId;} public void setTraceId(String v){traceId=v;}
    public String getToolCode(){return toolCode;} public void setToolCode(String v){toolCode=v;} public String getBusinessObjectId(){return businessObjectId;} public void setBusinessObjectId(String v){businessObjectId=v;}
    public String getOperationRequestId(){return operationRequestId;} public void setOperationRequestId(String v){operationRequestId=v;} public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    public String getInputHash(){return inputHash;} public void setInputHash(String v){inputHash=v;} public Long getApprovalId(){return approvalId;} public void setApprovalId(Long v){approvalId=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
    public String getResultJson(){return resultJson;} public void setResultJson(String v){resultJson=v;} public String getLastErrorCode(){return lastErrorCode;} public void setLastErrorCode(String v){lastErrorCode=v;}
    public String getLastErrorMessage(){return lastErrorMessage;} public void setLastErrorMessage(String v){lastErrorMessage=v;} public String getRetryAction(){return retryAction;} public void setRetryAction(String v){retryAction=v;}
    public Integer getRecoveryAttemptCount(){return recoveryAttemptCount;} public void setRecoveryAttemptCount(Integer v){recoveryAttemptCount=v;} public LocalDateTime getLastRecoveryAt(){return lastRecoveryAt;} public void setLastRecoveryAt(LocalDateTime v){lastRecoveryAt=v;}
    public Integer getVersion(){return version;} public void setVersion(Integer v){version=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public boolean isFreshExecution(){return freshExecution;} public void setFreshExecution(boolean v){freshExecution=v;}
}
