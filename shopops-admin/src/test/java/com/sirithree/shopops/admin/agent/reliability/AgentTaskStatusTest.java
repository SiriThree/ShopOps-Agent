package com.sirithree.shopops.admin.agent.reliability;

import static org.junit.jupiter.api.Assertions.*;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;
import org.junit.jupiter.api.Test;

class AgentTaskStatusTest {
    @Test void rejectsIllegalTerminalTransition() { assertFalse(AgentTaskStatus.SUCCEEDED.canTransitTo(AgentTaskStatus.RUNNING)); }
    @Test void allowsCancellationFlow() { assertTrue(AgentTaskStatus.RUNNING.canTransitTo(AgentTaskStatus.CANCEL_REQUESTED)); assertTrue(AgentTaskStatus.CANCEL_REQUESTED.canTransitTo(AgentTaskStatus.CANCELLED)); }
    @Test void unknownExternalResultCanRequireManualAction() { assertTrue(AgentTaskStatus.RUNNING.canTransitTo(AgentTaskStatus.NEEDS_MANUAL_ACTION)); }
}
