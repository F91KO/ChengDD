package com.cdd.config.infrastructure.persistence;

public record ConfigKvRecord(
        String configGroup,
        String configKey,
        String configValue,
        String configDesc,
        String merchantId) {
}
