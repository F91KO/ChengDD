package com.cdd.config.service;

import com.cdd.api.config.model.ConfigPublishConfigValueResponse;
import com.cdd.api.config.model.ConfigPublishRecordResponse;
import com.cdd.api.config.model.ConfigPublishRequest;
import com.cdd.api.config.model.ConfigPublishRollbackRequest;
import com.cdd.api.config.model.ConfigPublishSnapshotResponse;
import com.cdd.api.config.model.ConfigPublishStepResponse;
import com.cdd.api.config.model.ConfigKvUpsertRequest;
import com.cdd.api.config.model.ConfigKvValueResponse;
import com.cdd.api.config.model.FeatureSwitchStatusChangeRequest;
import com.cdd.api.config.model.FeatureSwitchUpsertRequest;
import com.cdd.api.config.model.FeatureSwitchValueResponse;
import com.cdd.api.config.model.MerchantConfigKvOverrideRequest;
import com.cdd.api.config.model.MerchantFeatureSwitchChangeRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.config.error.ConfigErrorCode;
import com.cdd.config.infrastructure.persistence.ConfigPublishRepository;
import com.cdd.config.infrastructure.persistence.ConfigKvRecord;
import com.cdd.config.infrastructure.persistence.ConfigKvRepository;
import com.cdd.config.infrastructure.persistence.FeatureSwitchDefinitionRecord;
import com.cdd.config.infrastructure.persistence.FeatureSwitchRepository;
import com.cdd.config.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String RELEASE_TYPE_CONFIG_PUBLISH = "config_publish";
    private static final String RELEASE_TYPE_CONFIG_ROLLBACK = "config_rollback";
    private static final String RELEASE_STATUS_SUCCESS = "success";
    private static final String RELEASE_TRIGGER_SOURCE = "config_service";
    private static final String RELEASE_STEP_STATUS_SUCCESS = "success";
    private static final String RELEASE_STEP_CODE_PUBLISH = "config_snapshot_publish";
    private static final String RELEASE_STEP_CODE_ROLLBACK = "config_snapshot_rollback";
    private static final Pattern NUMERIC_TAIL_PATTERN = Pattern.compile("(\\d+)$");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));
    private static final TypeReference<List<ConfigPublishConfigValueResponse>> CONFIG_VALUE_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<FeatureSwitchValueResponse>> SWITCH_VALUE_LIST_TYPE = new TypeReference<>() {
    };

    private final ConfigKvRepository configKvRepository;
    private final FeatureSwitchRepository featureSwitchRepository;
    private final ConfigPublishRepository configPublishRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public ConfigApplicationService(ConfigKvRepository configKvRepository,
                                    FeatureSwitchRepository featureSwitchRepository,
                                    ConfigPublishRepository configPublishRepository,
                                    IdGenerator idGenerator,
                                    ObjectMapper objectMapper) {
        this.configKvRepository = configKvRepository;
        this.featureSwitchRepository = featureSwitchRepository;
        this.configPublishRepository = configPublishRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
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

    public List<ConfigPublishRecordResponse> listPublishRecords(String merchantId, String storeId) {
        Long merchantNumericId = parseScopeNumericId(merchantId, "商家标识");
        Long storeNumericId = StringUtils.hasText(storeId) ? parseScopeNumericId(storeId, "店铺标识") : null;
        return configPublishRepository.listTasks(merchantNumericId, storeNumericId).stream()
                .map(task -> toPublishRecordResponse(task, false))
                .toList();
    }

    @Transactional
    public ConfigPublishRecordResponse createPublishRecord(ConfigPublishRequest request) {
        String merchantId = trimRequired(request.merchantId());
        String storeId = trimRequired(request.storeId());
        long merchantNumericId = parseScopeNumericId(merchantId, "商家标识");
        long storeNumericId = parseScopeNumericId(storeId, "店铺标识");
        Instant now = Instant.now();
        ConfigPublishSnapshotResponse snapshot = buildSnapshot(merchantId);
        String taskNo = buildTaskNo("CFG");
        long taskId = idGenerator.nextId();

        configPublishRepository.createTask(new ConfigPublishRepository.PublishTaskRecord(
                taskId,
                taskNo,
                merchantNumericId,
                storeNumericId,
                RELEASE_TYPE_CONFIG_PUBLISH,
                RELEASE_STATUS_SUCCESS,
                RELEASE_TRIGGER_SOURCE,
                writeSnapshot(snapshot, merchantId, storeId, request.operatorName(), request.publishNote(), null, null),
                RELEASE_STEP_CODE_PUBLISH,
                RELEASE_STATUS_SUCCESS,
                null,
                null,
                null,
                now,
                now,
                now));
        configPublishRepository.createStep(new ConfigPublishRepository.PublishStepRecord(
                idGenerator.nextId(),
                taskId,
                RELEASE_STEP_CODE_PUBLISH,
                "配置快照发布",
                1,
                RELEASE_STEP_STATUS_SUCCESS,
                "已完成配置快照采集与发布记录落库",
                null,
                0,
                now,
                now));
        return getPublishRecord(taskNo);
    }

    public ConfigPublishRecordResponse getPublishRecord(String taskNo) {
        ConfigPublishRepository.PublishTaskRecord task = configPublishRepository.findTaskByTaskNo(trimRequired(taskNo))
                .orElseThrow(() -> new BusinessException(ConfigErrorCode.CONFIG_PUBLISH_RECORD_NOT_FOUND));
        return toPublishRecordResponse(task, true);
    }

    @Transactional
    public ConfigPublishRecordResponse rollbackPublishRecord(String taskNo, ConfigPublishRollbackRequest request) {
        ConfigPublishRepository.PublishTaskRecord targetTask = configPublishRepository.findTaskByTaskNo(trimRequired(taskNo))
                .orElseThrow(() -> new BusinessException(ConfigErrorCode.CONFIG_PUBLISH_RECORD_NOT_FOUND));
        if (!RELEASE_TYPE_CONFIG_PUBLISH.equals(targetTask.releaseType())) {
            throw new BusinessException(ConfigErrorCode.CONFIG_PUBLISH_RECORD_NOT_FOUND, "仅配置发布记录支持回滚");
        }
        ConfigPublishSnapshotResponse snapshot = readSnapshot(targetTask.releaseSnapshotJson());
        restoreSnapshot(snapshot, requireMetadata(targetTask.releaseSnapshotJson()).merchantId());

        Instant now = Instant.now();
        String rollbackTaskNo = buildTaskNo("CFR");
        long rollbackTaskId = idGenerator.nextId();
        SnapshotMetadata targetMetadata = requireMetadata(targetTask.releaseSnapshotJson());

        configPublishRepository.createTask(new ConfigPublishRepository.PublishTaskRecord(
                rollbackTaskId,
                rollbackTaskNo,
                targetTask.merchantId(),
                targetTask.storeId(),
                RELEASE_TYPE_CONFIG_ROLLBACK,
                RELEASE_STATUS_SUCCESS,
                RELEASE_TRIGGER_SOURCE,
                writeSnapshot(
                        snapshot,
                        targetMetadata.merchantId(),
                        targetMetadata.storeId(),
                        request.operatorName(),
                        targetMetadata.publishNote(),
                        request.rollbackReason(),
                        targetTask.taskNo()),
                RELEASE_STEP_CODE_ROLLBACK,
                RELEASE_STATUS_SUCCESS,
                null,
                null,
                null,
                now,
                now,
                now));
        configPublishRepository.createStep(new ConfigPublishRepository.PublishStepRecord(
                idGenerator.nextId(),
                rollbackTaskId,
                RELEASE_STEP_CODE_ROLLBACK,
                "配置快照回滚",
                1,
                RELEASE_STEP_STATUS_SUCCESS,
                "已按目标发布快照恢复配置状态",
                null,
                0,
                now,
                now));
        configPublishRepository.createRollbackRecord(new ConfigPublishRepository.PublishRollbackRecord(
                idGenerator.nextId(),
                rollbackTaskId,
                targetTask.taskNo(),
                trimRequired(request.rollbackReason()),
                RELEASE_STATUS_SUCCESS,
                now));
        configPublishRepository.updateRollbackTaskNo(targetTask.id(), rollbackTaskNo);
        return getPublishRecord(rollbackTaskNo);
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

    private ConfigPublishRecordResponse toPublishRecordResponse(ConfigPublishRepository.PublishTaskRecord task, boolean includeDetail) {
        SnapshotMetadata metadata = requireMetadata(task.releaseSnapshotJson());
        ConfigPublishSnapshotResponse snapshot = includeDetail ? readSnapshot(task.releaseSnapshotJson()) : null;
        List<ConfigPublishStepResponse> steps = includeDetail
                ? configPublishRepository.listSteps(task.id()).stream().map(this::toPublishStepResponse).toList()
                : null;
        ConfigPublishRepository.PublishRollbackRecord rollbackRecord = RELEASE_TYPE_CONFIG_ROLLBACK.equals(task.releaseType())
                ? configPublishRepository.findRollbackRecord(task.id()).orElse(null)
                : null;
        int configCount = metadata.platformConfigCount() + metadata.merchantOverrideCount();
        int featureSwitchCount = metadata.platformFeatureSwitchCount() + metadata.merchantFeatureSwitchCount();
        return new ConfigPublishRecordResponse(
                task.taskNo(),
                metadata.merchantId(),
                metadata.storeId(),
                task.releaseType(),
                task.releaseStatus(),
                task.triggerSource(),
                metadata.operatorName(),
                metadata.publishNote(),
                rollbackRecord == null ? metadata.rollbackReason() : rollbackRecord.rollbackReason(),
                rollbackRecord == null ? metadata.rollbackTargetTaskNo() : rollbackRecord.rollbackTargetTaskNo(),
                task.rollbackTaskNo(),
                formatDateTime(task.startedAt()),
                formatDateTime(task.finishedAt()),
                formatDateTime(task.createdAt()),
                configCount,
                featureSwitchCount,
                snapshot,
                steps);
    }

    private ConfigPublishStepResponse toPublishStepResponse(ConfigPublishRepository.PublishStepRecord step) {
        return new ConfigPublishStepResponse(
                step.stepCode(),
                step.stepName(),
                step.stepOrder(),
                step.stepStatus(),
                step.resultMessage(),
                step.errorCode(),
                step.retryCount(),
                formatDateTime(step.startedAt()),
                formatDateTime(step.finishedAt()));
    }

    private ConfigPublishSnapshotResponse buildSnapshot(String merchantId) {
        List<ConfigPublishConfigValueResponse> platformConfigs = configKvRepository.listPlatform().stream()
                .map(record -> new ConfigPublishConfigValueResponse(
                        record.configGroup(),
                        record.configKey(),
                        record.configValue(),
                        record.configDesc(),
                        SOURCE_PLATFORM_DEFAULT,
                        null))
                .toList();
        List<ConfigPublishConfigValueResponse> merchantOverrides = configKvRepository.listMerchantOverrides(merchantId).stream()
                .map(record -> new ConfigPublishConfigValueResponse(
                        record.configGroup(),
                        record.configKey(),
                        record.configValue(),
                        record.configDesc(),
                        SOURCE_MERCHANT_OVERRIDE,
                        record.merchantId()))
                .toList();
        return new ConfigPublishSnapshotResponse(
                platformConfigs,
                merchantOverrides,
                listPlatformFeatureSwitches(),
                listMerchantFeatureSwitches(merchantId));
    }

    private void restoreSnapshot(ConfigPublishSnapshotResponse snapshot, String merchantId) {
        configKvRepository.softDeletePlatformNotIn(snapshot.platformConfigs().stream()
                .map(item -> new ConfigKvRecord(
                        item.configGroup(),
                        item.configKey(),
                        item.configValue(),
                        item.configDesc(),
                        null))
                .toList());
        snapshot.platformConfigs().forEach(item -> configKvRepository.upsertPlatform(
                idGenerator.nextId(),
                item.configGroup(),
                item.configKey(),
                item.configValue(),
                item.configDesc()));

        featureSwitchRepository.softDeleteDefinitionsNotIn(snapshot.platformFeatureSwitches().stream()
                .map(FeatureSwitchValueResponse::switchCode)
                .toList());
        snapshot.platformFeatureSwitches().forEach(item -> featureSwitchRepository.upsertDefinition(
                idGenerator.nextId(),
                item.switchCode(),
                item.switchName(),
                item.switchScope(),
                item.defaultValue(),
                item.status()));

        configKvRepository.softDeleteMerchantOverrides(merchantId);
        snapshot.merchantOverrides().forEach(item -> configKvRepository.upsertMerchantOverride(
                idGenerator.nextId(),
                merchantId,
                item.configGroup(),
                item.configKey(),
                item.configValue()));

        featureSwitchRepository.softDeleteMerchantOverrides(merchantId);
        snapshot.merchantFeatureSwitches().stream()
                .filter(item -> SOURCE_MERCHANT_OVERRIDE.equals(item.source()))
                .forEach(item -> {
                    FeatureSwitchDefinitionRecord definition = requireSwitchDefinition(item.switchCode());
                    featureSwitchRepository.upsertMerchantOverride(
                            idGenerator.nextId(),
                            definition.id(),
                            merchantId,
                            item.effectiveValue());
                });
    }

    private String writeSnapshot(ConfigPublishSnapshotResponse snapshot,
                                 String merchantId,
                                 String storeId,
                                 String operatorName,
                                 String publishNote,
                                 String rollbackReason,
                                 String rollbackTargetTaskNo) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            SnapshotMetadata metadata = new SnapshotMetadata(
                    trimRequired(merchantId),
                    trimRequired(storeId),
                    trimRequired(operatorName),
                    trimRequired(publishNote),
                    trimOptional(rollbackReason),
                    trimOptional(rollbackTargetTaskNo),
                    snapshot.platformConfigs().size(),
                    snapshot.merchantOverrides().size(),
                    snapshot.platformFeatureSwitches().size(),
                    snapshot.merchantFeatureSwitches().size());
            root.set("metadata", objectMapper.valueToTree(metadata));
            root.set("platform_configs", objectMapper.valueToTree(snapshot.platformConfigs()));
            root.set("merchant_overrides", objectMapper.valueToTree(snapshot.merchantOverrides()));
            root.set("platform_feature_switches", objectMapper.valueToTree(snapshot.platformFeatureSwitches()));
            root.set("merchant_feature_switches", objectMapper.valueToTree(snapshot.merchantFeatureSwitches()));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("配置发布快照序列化失败", ex);
        }
    }

    private ConfigPublishSnapshotResponse readSnapshot(String snapshotJson) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            return new ConfigPublishSnapshotResponse(
                    objectMapper.convertValue(root.path("platform_configs"), CONFIG_VALUE_LIST_TYPE),
                    objectMapper.convertValue(root.path("merchant_overrides"), CONFIG_VALUE_LIST_TYPE),
                    objectMapper.convertValue(root.path("platform_feature_switches"), SWITCH_VALUE_LIST_TYPE),
                    objectMapper.convertValue(root.path("merchant_feature_switches"), SWITCH_VALUE_LIST_TYPE));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("配置发布快照解析失败", ex);
        }
    }

    private SnapshotMetadata requireMetadata(String snapshotJson) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            return objectMapper.treeToValue(root.path("metadata"), SnapshotMetadata.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("配置发布元数据解析失败", ex);
        }
    }

    private long parseScopeNumericId(String raw, String fieldName) {
        String normalized = trimRequired(raw);
        Matcher matcher = NUMERIC_TAIL_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            throw new BusinessException(ConfigErrorCode.CONFIG_SCOPE_INVALID, fieldName + "需包含数值后缀");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String buildTaskNo(String prefix) {
        return prefix + idGenerator.nextId();
    }

    private String formatDateTime(Instant value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private record SnapshotMetadata(
            String merchantId,
            String storeId,
            String operatorName,
            String publishNote,
            String rollbackReason,
            String rollbackTargetTaskNo,
            int platformConfigCount,
            int merchantOverrideCount,
            int platformFeatureSwitchCount,
            int merchantFeatureSwitchCount) {
    }
}
