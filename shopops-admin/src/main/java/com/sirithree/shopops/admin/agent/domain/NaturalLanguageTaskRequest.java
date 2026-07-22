package com.sirithree.shopops.admin.agent.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class NaturalLanguageTaskRequest {
    @NotBlank
    private String userInput;

    @Valid
    private DateRangeParam dateRange;

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public DateRangeParam getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRangeParam dateRange) {
        this.dateRange = dateRange;
    }
}
