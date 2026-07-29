package com.sirithree.shopops.admin.agent.domain;

import java.util.ArrayList;
import java.util.List;

public class AgentTaskInterpretation {
    private AgentTaskSpec taskSpec;
    private double confidence;
    private String intentLabel;
    private String routedReason;
    private List<String> dataSources = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();

    public AgentTaskSpec getTaskSpec() {
        return taskSpec;
    }

    public void setTaskSpec(AgentTaskSpec taskSpec) {
        this.taskSpec = taskSpec;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getIntentLabel() {
        return intentLabel;
    }

    public void setIntentLabel(String intentLabel) {
        this.intentLabel = intentLabel;
    }

    public String getRoutedReason() {
        return routedReason;
    }

    public void setRoutedReason(String routedReason) {
        this.routedReason = routedReason;
    }

    public List<String> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<String> dataSources) {
        this.dataSources = dataSources == null ? new ArrayList<>() : new ArrayList<>(dataSources);
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions == null ? new ArrayList<>() : new ArrayList<>(recommendedActions);
    }
}
