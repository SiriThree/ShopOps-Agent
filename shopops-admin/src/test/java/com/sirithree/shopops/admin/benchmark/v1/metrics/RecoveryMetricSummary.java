package com.sirithree.shopops.admin.benchmark.v1.metrics;

public class RecoveryMetricSummary {
    public int faultCases;
    public int executedCases;
    public int terminalReached;
    public int stateCorrect;
    public int converged;
    public int permanentStuck;
    public int incorrectTerminalState;
    public int manualReview;
    public int duplicateSideEffects;
    public int totalRecoveryAttempts;
    public Double terminalConvergenceRate;
    public Double stateCorrectnessRate;
    public Double permanentStuckRate;
    public Double automaticRecoveryRate;
}
