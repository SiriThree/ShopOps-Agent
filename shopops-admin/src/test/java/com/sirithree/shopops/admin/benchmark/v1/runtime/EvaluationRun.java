package com.sirithree.shopops.admin.benchmark.v1.runtime;

import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import java.util.ArrayList;
import java.util.List;
import com.sirithree.shopops.admin.benchmark.v1.metrics.TaskMetricSummary;
import com.sirithree.shopops.admin.benchmark.v1.metrics.IdempotencyMetricSummary;
import com.sirithree.shopops.admin.benchmark.v1.metrics.RecoveryMetricSummary;
import com.sirithree.shopops.admin.benchmark.v1.metrics.GovernanceMetricSummary;

public class EvaluationRun {
    public EvaluationRunMetadata metadata;
    public List<EvaluationRecord> caseExecutions = new ArrayList<>();
    public AggregateReport aggregate = new AggregateReport();
    public TaskMetricSummary taskMetrics = new TaskMetricSummary();
    public IdempotencyMetricSummary idempotencyMetrics = new IdempotencyMetricSummary();
    public RecoveryMetricSummary recoveryMetrics = new RecoveryMetricSummary();
    public GovernanceMetricSummary governanceMetrics = new GovernanceMetricSummary();
}
