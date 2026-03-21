package com.cdd.config.infrastructure.persistence;

public record FeatureSwitchDefinitionRecord(
        long id,
        String switchCode,
        String switchName,
        String switchScope,
        String defaultValue,
        String status) {
}
