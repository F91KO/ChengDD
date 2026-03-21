package com.cdd.agent.core.model;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TaskInstanceTest {

    @Test
    void shouldTrackMainLifecycleAndRetry() {
        TaskInstance instance = new TaskInstance("TASK_1001", definition());
        LocalDateTime now = LocalDateTime.now();
        instance.transitionTo(TaskStatus.READY, now);
        instance.transitionTo(TaskStatus.RUNNING, now.plusSeconds(1));
        instance.markFailed("执行步骤失败", now.plusSeconds(2));
        instance.incrementRetryCount();
        instance.transitionTo(TaskStatus.READY, now.plusSeconds(3));

        Assertions.assertEquals(TaskStatus.READY, instance.status());
        Assertions.assertEquals(1, instance.retryCount());
        Assertions.assertNotNull(instance.startedAt());
    }

    @Test
    void shouldRejectRetryWhenNotFailed() {
        TaskInstance instance = new TaskInstance("TASK_1002", definition());
        Assertions.assertThrows(IllegalStateException.class, instance::incrementRetryCount);
    }

    private static TaskDefinition definition() {
        return new TaskDefinition(
                "publish_task",
                "发布任务",
                "release",
                3,
                600,
                false,
                true,
                List.of(new TaskStepDefinition("STEP_A", "步骤A", 1, "tool_a", false, false)));
    }
}
