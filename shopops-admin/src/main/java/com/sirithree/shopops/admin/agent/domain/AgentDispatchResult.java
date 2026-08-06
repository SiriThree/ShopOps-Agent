package com.sirithree.shopops.admin.agent.domain;

public class AgentDispatchResult {
    private final boolean synchronous;
    private final AgentExecutionResult executionResult;

    private AgentDispatchResult(boolean synchronous, AgentExecutionResult executionResult) {
        this.synchronous = synchronous;
        this.executionResult = executionResult;
    }

    public static AgentDispatchResult completed(AgentExecutionResult executionResult) {
        return new AgentDispatchResult(true, executionResult);
    }

    public static AgentDispatchResult accepted() {
        return new AgentDispatchResult(false, null);
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public boolean isAsynchronous() {
        return !synchronous;
    }

    public AgentExecutionResult getExecutionResult() {
        return executionResult;
    }
}
