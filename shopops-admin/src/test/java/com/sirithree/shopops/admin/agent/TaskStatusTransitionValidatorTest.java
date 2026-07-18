package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;
import com.sirithree.shopops.admin.agent.service.TaskStatusTransitionValidator;
import org.junit.jupiter.api.Test;

class TaskStatusTransitionValidatorTest {
    @Test
    void shouldAllowExpectedTaskStatusTransitions() {
        assertThatCode(() -> TaskStatusTransitionValidator.requireTransition("CREATED", AgentTaskStatus.RUNNING))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskStatusTransitionValidator.requireTransition("CREATED", AgentTaskStatus.FAILED))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskStatusTransitionValidator.requireTransition("RUNNING", AgentTaskStatus.SUCCESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskStatusTransitionValidator.requireTransition("RUNNING", AgentTaskStatus.DEGRADED))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskStatusTransitionValidator.requireTransition("RUNNING", AgentTaskStatus.FAILED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTerminalAndSkippedTaskStatusTransitions() {
        assertThatThrownBy(() -> TaskStatusTransitionValidator.requireTransition("CREATED", AgentTaskStatus.SUCCESS))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TaskStatusTransitionValidator.requireTransition("SUCCESS", AgentTaskStatus.FAILED))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TaskStatusTransitionValidator.requireTransition("FAILED", AgentTaskStatus.RUNNING))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TaskStatusTransitionValidator.requireTransition("DEGRADED", AgentTaskStatus.SUCCESS))
                .isInstanceOf(IllegalStateException.class);
    }
}
