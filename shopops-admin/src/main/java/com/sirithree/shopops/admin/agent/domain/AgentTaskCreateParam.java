package com.sirithree.shopops.admin.agent.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentTaskCreateParam {
    @NotBlank
    private String taskType;

    @NotBlank
    private String userInput;

    private String intent;

    @Valid
    @NotNull
    private DateRangeParam dateRange;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public DateRangeParam getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRangeParam dateRange) {
        this.dateRange = dateRange;
    }
}
