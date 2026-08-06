package com.sirithree.shopops.admin.connector.domain;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class ConnectorApiCallLogQueryParam {
    private Long logId;
    private Long jobId;
    private String connectorCode;
    private String endpoint;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdStart;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getConnectorCode() { return connectorCode; }
    public void setConnectorCode(String connectorCode) { this.connectorCode = connectorCode; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedStart() { return createdStart; }
    public void setCreatedStart(LocalDateTime createdStart) { this.createdStart = createdStart; }
    public LocalDateTime getCreatedEnd() { return createdEnd; }
    public void setCreatedEnd(LocalDateTime createdEnd) { this.createdEnd = createdEnd; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public int offset() {
        return (safePageNum() - 1) * safePageSize();
    }
}
