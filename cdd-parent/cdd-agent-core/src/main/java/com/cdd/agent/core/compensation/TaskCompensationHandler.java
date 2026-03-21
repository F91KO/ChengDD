package com.cdd.agent.core.compensation;

import com.cdd.agent.core.context.TaskContext;
import com.cdd.agent.core.model.TaskInstance;

/**
 * 补偿处理接口。
 */
public interface TaskCompensationHandler {

    TaskCompensationResult compensate(TaskInstance taskInstance, TaskContext context);
}
