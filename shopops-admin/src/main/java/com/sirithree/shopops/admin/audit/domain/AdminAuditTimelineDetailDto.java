package com.sirithree.shopops.admin.audit.domain;

import java.util.Map;

public class AdminAuditTimelineDetailDto {
    private AdminAuditTimelineEventDto event;
    private Map<String, Object> resource;
    private Map<String, Object> context;

    public AdminAuditTimelineEventDto getEvent() {
        return event;
    }

    public void setEvent(AdminAuditTimelineEventDto event) {
        this.event = event;
    }

    public Map<String, Object> getResource() {
        return resource;
    }

    public void setResource(Map<String, Object> resource) {
        this.resource = resource;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
