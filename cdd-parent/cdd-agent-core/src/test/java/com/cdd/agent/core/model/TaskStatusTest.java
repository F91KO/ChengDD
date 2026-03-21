package com.cdd.agent.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TaskStatusTest {

    @Test
    void shouldAllowConfiguredTransitions() {
        Assertions.assertTrue(TaskStatus.PENDING.canTransitionTo(TaskStatus.READY));
        Assertions.assertTrue(TaskStatus.READY.canTransitionTo(TaskStatus.RUNNING));
        Assertions.assertTrue(TaskStatus.RUNNING.canTransitionTo(TaskStatus.WAITING_APPROVAL));
        Assertions.assertTrue(TaskStatus.FAILED.canTransitionTo(TaskStatus.COMPENSATING));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        Assertions.assertFalse(TaskStatus.PENDING.canTransitionTo(TaskStatus.RUNNING));
        Assertions.assertThrows(IllegalStateException.class,
                () -> TaskStatus.PENDING.validateTransitionTo(TaskStatus.SUCCEEDED));
    }

    @Test
    void shouldMarkTerminalStates() {
        Assertions.assertTrue(TaskStatus.SUCCEEDED.isTerminal());
        Assertions.assertTrue(TaskStatus.COMPENSATED.isTerminal());
        Assertions.assertTrue(TaskStatus.CANCELED.isTerminal());
        Assertions.assertFalse(TaskStatus.FAILED.isTerminal());
    }
}
