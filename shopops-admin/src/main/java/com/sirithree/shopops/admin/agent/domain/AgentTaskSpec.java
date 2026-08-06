package com.sirithree.shopops.admin.agent.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentTaskSpec {
    private String intent;
    private String objective;
    private DateRangeParam dateRange;
    private List<String> focusAreas = new ArrayList<>();
    private List<String> requiredEvidence = new ArrayList<>();
    private String outputFormat;
    private Map<String, Object> constraints = new LinkedHashMap<>();

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public DateRangeParam getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRangeParam dateRange) {
        this.dateRange = dateRange;
    }

    public List<String> getFocusAreas() {
        return focusAreas;
    }

    public void setFocusAreas(List<String> focusAreas) {
        this.focusAreas = focusAreas == null ? new ArrayList<>() : new ArrayList<>(focusAreas);
    }

    public List<String> getRequiredEvidence() {
        return requiredEvidence;
    }

    public void setRequiredEvidence(List<String> requiredEvidence) {
        this.requiredEvidence = requiredEvidence == null ? new ArrayList<>() : new ArrayList<>(requiredEvidence);
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints == null ? new LinkedHashMap<>() : new LinkedHashMap<>(constraints);
    }
}
