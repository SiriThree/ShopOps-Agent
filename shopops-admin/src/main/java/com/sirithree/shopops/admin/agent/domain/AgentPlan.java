package com.sirithree.shopops.admin.agent.domain;

import java.util.ArrayList;
import java.util.List;

public class AgentPlan {
    private String taskType;
    private String rationale;
    private List<AgentPlanStep> steps = new ArrayList<>();

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<AgentPlanStep> getSteps() {
        return steps;
    }

    public void setSteps(List<AgentPlanStep> steps) {
        this.steps = steps;
    }
}
