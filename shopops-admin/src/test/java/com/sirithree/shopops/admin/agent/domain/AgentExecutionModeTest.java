package com.sirithree.shopops.admin.agent.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AgentExecutionModeTest {
    @Test void defaultsToAdvisory() { assertEquals(AgentExecutionMode.ADVISORY, AgentExecutionMode.from(null)); }
    @Test void rejectsUnknownMode() { assertThrows(IllegalArgumentException.class, () -> AgentExecutionMode.from("ROOT")); }
}
