package com.sirithree.shopops.admin.agent.domain;

public class AgentTaskCreateResult {
    private Long taskId;
    private String taskNo;
    private String status;
    private String traceId;

    public AgentTaskCreateResult(Long taskId, String taskNo, String status, String traceId) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.status = status;
        this.traceId = traceId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
