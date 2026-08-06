package com.sirithree.shopops.admin.agent.domain;

import jakarta.validation.constraints.NotBlank;

public class DateRangeParam {
    @NotBlank
    private String start;

    @NotBlank
    private String end;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
