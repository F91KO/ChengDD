package com.cdd.agent.core.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TaskDefinitionTest {

    @Test
    void shouldRejectDuplicateStepCode() {
        TaskStepDefinition first = new TaskStepDefinition("S1", "步骤1", 1, "tool_a", false, false);
        TaskStepDefinition duplicate = new TaskStepDefinition("S1", "步骤2", 2, "tool_b", false, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TaskDefinition(
                "publish_task",
                "发布任务",
                "release",
                3,
                600,
                false,
                true,
                List.of(first, duplicate)));
    }

    @Test
    void shouldCopyStepListForImmutability() {
        List<TaskStepDefinition> steps = new ArrayList<>();
        steps.add(new TaskStepDefinition("S1", "步骤1", 1, "tool_a", false, false));
        TaskDefinition definition = new TaskDefinition(
                "publish_task",
                "发布任务",
                "release",
                3,
                600,
                false,
                true,
                steps);
        steps.add(new TaskStepDefinition("S2", "步骤2", 2, "tool_b", false, false));
        Assertions.assertEquals(1, definition.steps().size());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> definition.steps().add(new TaskStepDefinition("S3", "步骤3", 3, "tool_c", false, false)));
    }
}
