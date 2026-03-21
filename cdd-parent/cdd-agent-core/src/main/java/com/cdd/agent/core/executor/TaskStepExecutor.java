package com.cdd.agent.core.executor;

import com.cdd.agent.core.context.TaskContext;
import com.cdd.agent.core.model.TaskExecutionResult;
import com.cdd.agent.core.model.TaskInstance;
import com.cdd.agent.core.model.TaskStepInstance;

/**
 * 任务步骤执行器接口。
 */
public interface TaskStepExecutor {

    TaskExecutionResult execute(TaskInstance taskInstance, TaskStepInstance stepInstance, TaskContext context);
}
