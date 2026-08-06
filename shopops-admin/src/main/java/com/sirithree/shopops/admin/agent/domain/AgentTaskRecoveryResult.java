package com.sirithree.shopops.admin.agent.domain;

import java.util.List;

public class AgentTaskRecoveryResult {
    private int scannedCount;
    private int requeuedCount;
    private List<Long> taskIds = List.of();

    public int getScannedCount() {
        return scannedCount;
    }

    public void setScannedCount(int scannedCount) {
        this.scannedCount = scannedCount;
    }

    public int getRequeuedCount() {
        return requeuedCount;
    }

    public void setRequeuedCount(int requeuedCount) {
        this.requeuedCount = requeuedCount;
    }

    public List<Long> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<Long> taskIds) {
        this.taskIds = taskIds;
    }
}
