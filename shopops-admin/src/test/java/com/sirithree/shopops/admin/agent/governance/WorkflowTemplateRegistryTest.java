package com.sirithree.shopops.admin.agent.governance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorkflowTemplateRegistryTest {
    @Test void rejectsUnknownWorkflow() { assertThrows(IllegalArgumentException.class, () -> new WorkflowTemplateRegistry().require("arbitrary")); }
    @Test void dailyReviewHasBoundedRepair() { assertEquals(1, new WorkflowTemplateRegistry().require("daily_review").maxRepairAttempts()); }
}
