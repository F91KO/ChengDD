package com.cdd.config.service;

import com.cdd.api.config.model.ConfigKvUpsertRequest;
import com.cdd.api.config.model.ConfigKvValueResponse;
import com.cdd.api.config.model.FeatureSwitchStatusChangeRequest;
import com.cdd.api.config.model.FeatureSwitchUpsertRequest;
import com.cdd.api.config.model.FeatureSwitchValueResponse;
import com.cdd.api.config.model.MerchantConfigKvOverrideRequest;
import com.cdd.api.config.model.MerchantFeatureSwitchChangeRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.config.error.ConfigErrorCode;
import com.cdd.config.infrastructure.persistence.ConfigKvRecord;
import com.cdd.config.infrastructure.persistence.ConfigKvRepository;
import com.cdd.config.infrastructure.persistence.FeatureSwitchDefinitionRecord;
import com.cdd.config.infrastructure.persistence.FeatureSwitchRepository;
import com.cdd.config.support.IdGenerator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConfigApplicationService {

    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final String SWITCH_VALUE_ON = "on";
    private static final String SWITCH_VALUE_OFF = "off";

    private static final String SOURCE_PLATFORM_DEFAULT = "platform_default";
    private static final String SOURCE_MERCHANT_OVERRIDE = "merchant_override";
    private static final String SOURCE_PLATFORM_DISABLED = "platform_disabled";

    private final ConfigKvRepository configKvRepository;
    private final FeatureSwitchRepository featureSwitchRepository;
    private final IdGenerator idGenerator;

    public ConfigApplicationService(ConfigKvRepository configKvRepository,
                                    FeatureSwitchRepository featureSwitchRepository,
                                    IdGenerator idGenerator) {
        this.configKvRepository = configKvRepository;
        this.featureSwitchRepository = featureSwitchRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public ConfigKvValueResponse upsertPlatformConfig(ConfigKvUpsertRequest request) {
        String configGroup = trimRequired(request.configGroup());
        String configKey = trimRequired(request.configKey());
        String configValue = trimRequired(request.configValue());
        String configDesc = trimOptional(request.configDesc());
        configKvRepository.upsertPlatform(idGenerator.nextId(), configGroup, configKey, configValue, configDesc);
        return new ConfigKvValueResponse(configGroup, configKey, configValue, SOURCE_PLATFORM_DEFAULT, null);
    }

    @Transactional
    public ConfigKvValueResponse upsertMerchantConfigOverride(MerchantConfigKvOverrideRequest request) {
        String merchantId = trimRequired(request.merchantId());
        String configGroup = trimRequired(request.configGroup());
        String configKey = trimRequired(request.configKey());
        String configValue = trimRequired(request.configValue());
        ensurePlatformConfigExists(configGroup, configKey);
        configKvRepository.upsertMerchantOverride(idGenerator.nextId(), merchantId, configGroup, configKey, configValue);
        return new ConfigKvValueResponse(configGroup, configKey, configValue, SOURCE_MERCHANT_OVERRIDE, merchantId);
    }

    public ConfigKvValueResponse getEffectiveConfig(String merchantId, String configGroup, String configKey) {
        String normalizedMerchantId = trimOptional(merchantId);
        String normalizedGroup = trimRequired(configGroup);
        String normalizedKey = trimRequired(configKey);
        ConfigKvRecord platform = configKvRepository.findPlatform(normalizedGroup, normalizedKey)
                .orElseThrow(() -> new BusinessException(ConfigErrorCode.CONFIG_NOT_FOUND));
        if (StringUtils.hasText(normalizedMerchantId)) {
            return configKvRepository.findMerchantOverride(normalizedMerchantId, normalizedGroup, normalizedKey)
                    .map(record -> new ConfigKvValueResponse(
                            record.configGroup(),
                            record.configKey(),
                            record.configValue(),
                            SOURCE_MERCHANT_OVERRIDE,
                            record.merchantId()))
                    .orElseGet(() -> new ConfigKvValueResponse(
                            platform.configGroup(),
                            platform.configKey(),
                            platform.configValue(),
                            SOURCE_PLATFORM_DEFAULT,
                            normalizedMerchantId));
        }
        return new ConfigKvValueResponse(
                platform.configGroup(),
                platform.configKey(),
                platform.configValue(),
                SOURCE_PLATFORM_DEFAULT,
                null);
    }

    @Transactional
    public FeatureSwitchValueResponse upsertFeatureSwitch(FeatureSwitchUpsertRequest request) {
        String switchCode = normalizeSwitchCode(request.switchCode());
        String switchName = trimRequired(request.switchName());
        String switchScope = trimRequired(request.switchScope());
        String defaultValue = normalizeSwitchValue(request.defaultValue());
        String status = normalizeStatus(request.status());
        validateSwitchValue(defaultValue);
        validateStatus(status);
        featureSwitchRepository.upsertDefinition(
                idGenerator.nextId(),
                switchCode,
                switchName,
                switchScope,
                defaultValue,
                status);
        FeatureSwitchDefinitionRecord definition = requireSwitchDefinition(switchCode);
        return toPlatformResponse(definition);
    }

    @Transactional
    public FeatureSwitchValueResponse changeFeatureSwitchStatus(String switchCode,
                                                                FeatureSwitchStatusChangeRequest request) {
        String normalizedSwitchCode = normalizeSwitchCode(switchCode);
        String status = normalizeStatus(request.status());
        validateStatus(status);
        boolean updated = featureSwitchRepository.updateStatus(normalizedSwitchCode, status);
        if (!updated) {
            throw new BusinessException(ConfigErrorCode.FEATURE_SWITCH_NOT_FOUND);
        }
        return toPlatformResponse(requireSwitchDefinition(normalizedSwitchCode));
    }

    public FeatureSwitchValueResponse getPlatformFeatureSwitch(String switchCode) {
        return toPlatformResponse(requireSwitchDefinition(normalizeSwitchCode(switchCode)));
    }

    public List<FeatureSwitchValueResponse> listPlatformFeatureSwitches() {
        return featureSwitchRepository.findAll().stream()
                .map(this::toPlatformResponse)
                .toList();
    }

    @Transactional
    public FeatureSwitchValueResponse changeMerchantFeatureSwitch(String switchCode,
                                                                  MerchantFeatureSwitchChangeRequest request) {
        String normalizedSwitchCode = normalizeSwitchCode(switchCode);
        String merchantId = trimRequired(request.merchantId());
        String switchValue = normalizeSwitchValue(request.switchValue());
        validateSwitchValue(switchValue);
        FeatureSwitchDefinitionRecord definition = requireSwitchDefinition(normalizedSwitchCode);
        featureSwitchRepository.upsertMerchantOverride(idGenerator.nextId(), definition.id(), merchantId, switchValue);
        return toMerchantResponse(definition, merchantId);
    }

    public FeatureSwitchValueResponse getMerchantFeatureSwitch(String switchCode, String merchantId) {
        FeatureSwitchDefinitionRecord definition = requireSwitchDefinition(normalizeSwitchCode(switchCode));
        return toMerchantResponse(definition, trimRequired(merchantId));
    }

    public List<FeatureSwitchValueResponse> listMerchantFeatureSwitches(String merchantId) {
        String normalizedMerchantId = trimRequired(merchantId);
        return featureSwitchRepository.findAll().stream()
                .map(definition -> toMerchantResponse(definition, normalizedMerchantId))
                .toList();
    }

    private FeatureSwitchValueResponse toPlatformResponse(FeatureSwitchDefinitionRecord definition) {
        if (STATUS_DISABLED.equals(definition.status())) {
            return new FeatureSwitchValueResponse(
                    definition.switchCode(),
                    definition.switchName(),
                    definition.switchScope(),
                    definition.defaultValue(),
                    SWITCH_VALUE_OFF,
                    definition.status(),
                    SOURCE_PLATFORM_DISABLED,
                    null);
        }
        return new FeatureSwitchValueResponse(
                definition.switchCode(),
                definition.switchName(),
                definition.switchScope(),
                definition.defaultValue(),
                definition.defaultValue(),
                definition.status(),
                SOURCE_PLATFORM_DEFAULT,
                null);
    }

    private FeatureSwitchValueResponse toMerchantResponse(FeatureSwitchDefinitionRecord definition, String merchantId) {
        if (STATUS_DISABLED.equals(definition.status())) {
            return new FeatureSwitchValueResponse(
                    definition.switchCode(),
                    definition.switchName(),
                    definition.switchScope(),
                    definition.defaultValue(),
                    SWITCH_VALUE_OFF,
                    definition.status(),
                    SOURCE_PLATFORM_DISABLED,
                    merchantId);
        }
        return featureSwitchRepository.findMerchantOverride(definition.id(), merchantId)
                .map(override -> new FeatureSwitchValueResponse(
                        definition.switchCode(),
                        definition.switchName(),
                        definition.switchScope(),
                        definition.defaultValue(),
                        override.switchValue(),
                        definition.status(),
                        SOURCE_MERCHANT_OVERRIDE,
                        merchantId))
                .orElseGet(() -> new FeatureSwitchValueResponse(
                        definition.switchCode(),
                        definition.switchName(),
                        definition.switchScope(),
                        definition.defaultValue(),
                        definition.defaultValue(),
                        definition.status(),
                        SOURCE_PLATFORM_DEFAULT,
                        merchantId));
    }

    private FeatureSwitchDefinitionRecord requireSwitchDefinition(String switchCode) {
        return featureSwitchRepository.findByCode(switchCode)
                .orElseThrow(() -> new BusinessException(ConfigErrorCode.FEATURE_SWITCH_NOT_FOUND));
    }

    private void ensurePlatformConfigExists(String configGroup, String configKey) {
        if (configKvRepository.findPlatform(configGroup, configKey).isEmpty()) {
            throw new BusinessException(ConfigErrorCode.CONFIG_NOT_FOUND, "目标配置不存在，无法设置商家覆盖");
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ConfigErrorCode.FEATURE_SWITCH_STATUS_INVALID, "功能开关状态仅支持 enabled 或 disabled");
        }
    }

    private void validateSwitchValue(String value) {
        if (!SWITCH_VALUE_ON.equals(value) && !SWITCH_VALUE_OFF.equals(value)) {
            throw new BusinessException(ConfigErrorCode.FEATURE_SWITCH_VALUE_INVALID, "功能开关值仅支持 on 或 off");
        }
    }

    private static String normalizeStatus(String raw) {
        return normalizeLowerCase(raw);
    }

    private static String normalizeSwitchCode(String raw) {
        return normalizeLowerCase(raw);
    }

    private static String normalizeSwitchValue(String raw) {
        return normalizeLowerCase(raw);
    }

    private static String normalizeLowerCase(String raw) {
        return trimRequired(raw).toLowerCase(Locale.ROOT);
    }

    private static String trimRequired(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim();
    }

    private static String trimOptional(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }
}
