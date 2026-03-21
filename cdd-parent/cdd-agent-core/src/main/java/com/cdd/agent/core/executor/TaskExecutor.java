package com.cdd.agent.core.executor;

import com.cdd.agent.core.context.TaskContext;
import com.cdd.agent.core.model.TaskDefinition;
import com.cdd.agent.core.model.TaskExecutionResult;
import com.cdd.agent.core.model.TaskInstance;

/**
 * 任务执行器接口。
 */
public interface TaskExecutor {

    TaskExecutionResult execute(TaskDefinition definition, TaskInstance instance, TaskContext context);
}
