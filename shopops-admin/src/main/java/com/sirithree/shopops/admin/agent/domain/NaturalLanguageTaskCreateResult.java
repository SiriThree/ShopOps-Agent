package com.sirithree.shopops.admin.agent.domain;

import java.util.List;

public class NaturalLanguageTaskCreateResult {
    private String intent;
    private String intentLabel;
    private double confidence;
    private String taskType;
    private String routedReason;
    private List<String> focusAreas;
    private List<String> dataSources;
    private List<String> recommendedActions;
    private DateRangeParam dateRange;
    private AgentTaskCreateResult task;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getIntentLabel() {
        return intentLabel;
    }

    public void setIntentLabel(String intentLabel) {
        this.intentLabel = intentLabel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getRoutedReason() {
        return routedReason;
    }

    public void setRoutedReason(String routedReason) {
        this.routedReason = routedReason;
    }

    public List<String> getFocusAreas() {
        return focusAreas;
    }

    public void setFocusAreas(List<String> focusAreas) {
        this.focusAreas = focusAreas;
    }

    public List<String> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<String> dataSources) {
        this.dataSources = dataSources;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public DateRangeParam getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRangeParam dateRange) {
        this.dateRange = dateRange;
    }

    public AgentTaskCreateResult getTask() {
        return task;
    }

    public void setTask(AgentTaskCreateResult task) {
        this.task = task;
    }
}
