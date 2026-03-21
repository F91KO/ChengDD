package com.cdd.config.infrastructure.persistence;

import java.util.Optional;

public interface ConfigKvRepository {

    void upsertPlatform(long id, String configGroup, String configKey, String configValue, String configDesc);

    void upsertMerchantOverride(long id, String merchantId, String configGroup, String configKey, String configValue);

    Optional<ConfigKvRecord> findPlatform(String configGroup, String configKey);

    Optional<ConfigKvRecord> findMerchantOverride(String merchantId, String configGroup, String configKey);
}
