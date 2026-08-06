package com.sirithree.shopops.admin.agent.domain;

public enum AgentExecutionMode {
    ADVISORY,
    DRAFT,
    AUTOMATIC;

    public static AgentExecutionMode from(String value) {
        if (value == null || value.isBlank()) return ADVISORY;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("不支持的 Agent 执行模式: " + value); }
    }
}
