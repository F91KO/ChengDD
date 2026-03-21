package com.cdd.agent.core.context;

import java.util.Map;

/**
 * 任务执行上下文。
 */
public record TaskContext(
        String requestId,
        String operatorId,
        String operatorName,
        String merchantId,
        String storeId,
        Map<String, Object> attributes) {

    public TaskContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }
}
