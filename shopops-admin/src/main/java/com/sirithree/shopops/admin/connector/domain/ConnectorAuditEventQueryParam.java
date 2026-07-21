package com.sirithree.shopops.admin.connector.domain;

import java.time.LocalDateTime;

public class ConnectorAuditEventQueryParam {
    private Long eventId;
    private String connectorCode;
    private String eventType;
    private String eventStatus;
    private Long userId;
    private String username;
    private LocalDateTime createdStart;
    private LocalDateTime createdEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getConnectorCode() { return connectorCode; }
    public void setConnectorCode(String connectorCode) { this.connectorCode = connectorCode; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
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
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    public int offset() {
        return (safePageNum() - 1) * safePageSize();
    }
}
