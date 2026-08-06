package com.sirithree.shopops.admin.agent.domain;

import java.util.ArrayList;
import java.util.List;

public class AgentVerificationResult {
    private boolean passed;
    private double score;
    private List<AgentVerificationCheck> checks = new ArrayList<>();
    private boolean repairable;
    private List<String> missingEvidence = new ArrayList<>();
    private List<String> repairToolCodes = new ArrayList<>();

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public List<AgentVerificationCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<AgentVerificationCheck> checks) {
        this.checks = checks == null ? new ArrayList<>() : new ArrayList<>(checks);
    }

    public boolean isRepairable() {
        return repairable;
    }

    public void setRepairable(boolean repairable) {
        this.repairable = repairable;
    }

    public List<String> getMissingEvidence() {
        return missingEvidence;
    }

    public void setMissingEvidence(List<String> missingEvidence) {
        this.missingEvidence = missingEvidence == null ? new ArrayList<>() : new ArrayList<>(missingEvidence);
    }

    public List<String> getRepairToolCodes() {
        return repairToolCodes;
    }

    public void setRepairToolCodes(List<String> repairToolCodes) {
        this.repairToolCodes = repairToolCodes == null ? new ArrayList<>() : new ArrayList<>(repairToolCodes);
    }
}
