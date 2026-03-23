package com.cdd.config.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

public interface FeatureSwitchRepository {

    void upsertDefinition(long id,
                          String switchCode,
                          String switchName,
                          String switchScope,
                          String defaultValue,
                          String status);

    boolean updateStatus(String switchCode, String status);

    Optional<FeatureSwitchDefinitionRecord> findByCode(String switchCode);

    List<FeatureSwitchDefinitionRecord> findAll();

    void upsertMerchantOverride(long id, long switchId, String merchantId, String switchValue);

    Optional<FeatureSwitchMerchantOverrideRecord> findMerchantOverride(long switchId, String merchantId);

    void softDeleteDefinitionsNotIn(List<String> switchCodes);

    void softDeleteMerchantOverrides(String merchantId);
}
