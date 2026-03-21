package com.cdd.config.infrastructure.persistence;

public record FeatureSwitchMerchantOverrideRecord(
        long switchId,
        String merchantId,
        String switchValue) {
}
