package com.cdd.decoration.service;

import com.cdd.api.decoration.model.DecorationHomeModuleRequest;
import com.cdd.api.decoration.model.DecorationHomeModuleResponse;
import com.cdd.api.decoration.model.PublishDecorationRequest;
import com.cdd.api.decoration.model.RollbackDecorationRequest;
import com.cdd.api.decoration.model.SaveDecorationDraftRequest;
import com.cdd.api.decoration.model.StoreDecorationConfigResponse;
import com.cdd.common.core.error.BusinessException;
import com.cdd.decoration.error.DecorationErrorCode;
import com.cdd.decoration.infrastructure.persistence.DecorationRepository;
import com.cdd.decoration.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DecorationApplicationService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_ROLLBACKED = "rollbacked";
    private static final String BINDING_STATUS_DRAFT = "draft";
    private static final String BINDING_STATUS_BOUND = "bound";

    private final DecorationRepository decorationRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public DecorationApplicationService(DecorationRepository decorationRepository,
                                        IdGenerator idGenerator,
                                        ObjectMapper objectMapper) {
        this.decorationRepository = decorationRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public StoreDecorationConfigResponse getDecorationConfig(long merchantId, long storeId, long miniProgramId) {
        DecorationRepository.HomeConfigRecord config = decorationRepository.findHomeConfig(merchantId, storeId, miniProgramId)
                .orElseThrow(() -> new BusinessException(DecorationErrorCode.DECORATION_CONFIG_NOT_FOUND));
        return toResponse(config, decorationRepository.listHomeModules(config.id()));
    }

    @Transactional
    public StoreDecorationConfigResponse saveDraft(long storeId, SaveDecorationDraftRequest request) {
        validateHomeStyleMode(request.homeStyleMode());
        validateTemplateCode(request.homeTemplateCode());
        if (request.homeModules() == null || request.homeModules().isEmpty()) {
            throw new BusinessException(DecorationErrorCode.DECORATION_MODULES_REQUIRED);
        }
        DecorationRepository.HomeConfigRecord existing = decorationRepository.findHomeConfig(
                request.merchantId(),
                storeId,
                request.miniProgramId()).orElse(null);
        int versionNo = existing == null ? 0 : existing.versionNo();
        long configId = existing == null ? idGenerator.nextId() : existing.id();
        DecorationRepository.HomeConfigRecord config = decorationRepository.saveHomeConfig(new DecorationRepository.HomeConfigUpsertCommand(
                configId,
                request.merchantId(),
                storeId,
                request.miniProgramId(),
                trimRequired(request.homeTemplateCode()),
                normalizeStyleMode(request.homeStyleMode()),
                trimRequired(request.themeColor()),
                trimOptional(request.searchPlaceholder()),
                writeJson(request.headerServiceBar()),
                writeJson(request.brandHeroBlock()),
                trimOptional(request.deliveryPromiseText()),
                trimOptional(request.deliveryFeeText()),
                trimOptional(request.minimumOrderText()),
                trimOptional(request.announcementText()),
                STATUS_DRAFT,
                versionNo));
        decorationRepository.replaceHomeModules(
                request.merchantId(),
                storeId,
                config.id(),
                toModuleDrafts(request.homeModules()));
        decorationRepository.upsertTemplateBinding(
                idGenerator.nextId(),
                request.merchantId(),
                storeId,
                request.miniProgramId(),
                trimRequired(request.homeTemplateCode()),
                normalizeStyleMode(request.homeStyleMode()),
                BINDING_STATUS_DRAFT);
        return toResponse(config, decorationRepository.listHomeModules(config.id()));
    }

    @Transactional
    public StoreDecorationConfigResponse publish(long storeId, PublishDecorationRequest request) {
        DecorationRepository.HomeConfigRecord current = decorationRepository.findHomeConfig(
                request.merchantId(),
                storeId,
                request.miniProgramId()).orElseThrow(() -> new BusinessException(DecorationErrorCode.DECORATION_CONFIG_NOT_FOUND));
        List<DecorationRepository.HomeModuleRecord> currentModules = decorationRepository.listHomeModules(current.id());
        int nextVersion = current.versionNo() + 1;
        DecorationRepository.HomeConfigRecord published = decorationRepository.saveHomeConfig(new DecorationRepository.HomeConfigUpsertCommand(
                current.id(),
                current.merchantId(),
                current.storeId(),
                current.miniProgramId(),
                current.homeTemplateCode(),
                current.homeStyleMode(),
                current.themeColor(),
                current.searchPlaceholder(),
                current.headerServiceBarJson(),
                current.brandHeroBlockJson(),
                current.deliveryPromiseText(),
                current.deliveryFeeText(),
                current.minimumOrderText(),
                current.announcementText(),
                STATUS_PUBLISHED,
                nextVersion));
        StoreDecorationConfigResponse response = toResponse(published, currentModules);
        decorationRepository.createHomeVersion(
                idGenerator.nextId(),
                published.merchantId(),
                published.storeId(),
                published.id(),
                nextVersion,
                STATUS_PUBLISHED,
                writeSnapshot(response),
                Instant.now());
        decorationRepository.upsertTemplateBinding(
                idGenerator.nextId(),
                published.merchantId(),
                published.storeId(),
                published.miniProgramId(),
                published.homeTemplateCode(),
                published.homeStyleMode(),
                BINDING_STATUS_BOUND);
        return response;
    }

    @Transactional
    public StoreDecorationConfigResponse rollback(long storeId, RollbackDecorationRequest request) {
        DecorationRepository.HomeConfigRecord current = decorationRepository.findHomeConfig(
                request.merchantId(),
                storeId,
                request.miniProgramId()).orElseThrow(() -> new BusinessException(DecorationErrorCode.DECORATION_CONFIG_NOT_FOUND));
        List<DecorationRepository.HomeVersionRecord> versions = decorationRepository.listHomeVersions(current.id());
        if (versions.isEmpty()) {
            throw new BusinessException(DecorationErrorCode.DECORATION_VERSION_NOT_FOUND);
        }
        DecorationRepository.HomeVersionRecord target = versions.size() > 1 ? versions.get(1) : versions.get(0);
        StoreDecorationConfigResponse snapshot = readSnapshot(target.snapshotJson());
        int nextVersion = current.versionNo() + 1;
        DecorationRepository.HomeConfigRecord rollbacked = decorationRepository.saveHomeConfig(new DecorationRepository.HomeConfigUpsertCommand(
                current.id(),
                request.merchantId(),
                storeId,
                request.miniProgramId(),
                snapshot.homeTemplateCode(),
                snapshot.homeStyleMode(),
                snapshot.themeColor(),
                snapshot.searchPlaceholder(),
                writeJson(snapshot.headerServiceBar()),
                writeJson(snapshot.brandHeroBlock()),
                snapshot.deliveryPromiseText(),
                snapshot.deliveryFeeText(),
                snapshot.minimumOrderText(),
                snapshot.announcementText(),
                STATUS_ROLLBACKED,
                nextVersion));
        decorationRepository.replaceHomeModules(
                request.merchantId(),
                storeId,
                rollbacked.id(),
                toModuleDraftsFromResponse(snapshot.homeModules()));
        StoreDecorationConfigResponse response = toResponse(rollbacked, decorationRepository.listHomeModules(rollbacked.id()));
        decorationRepository.createHomeVersion(
                idGenerator.nextId(),
                rollbacked.merchantId(),
                rollbacked.storeId(),
                rollbacked.id(),
                nextVersion,
                STATUS_ROLLBACKED,
                writeSnapshot(response),
                Instant.now());
        decorationRepository.upsertTemplateBinding(
                idGenerator.nextId(),
                rollbacked.merchantId(),
                rollbacked.storeId(),
                rollbacked.miniProgramId(),
                rollbacked.homeTemplateCode(),
                rollbacked.homeStyleMode(),
                BINDING_STATUS_BOUND);
        return response;
    }

    public StoreDecorationConfigResponse preview(long storeId, SaveDecorationDraftRequest request) {
        validateHomeStyleMode(request.homeStyleMode());
        validateTemplateCode(request.homeTemplateCode());
        if (request.homeModules() == null || request.homeModules().isEmpty()) {
            throw new BusinessException(DecorationErrorCode.DECORATION_MODULES_REQUIRED);
        }
        List<DecorationHomeModuleResponse> modules = request.homeModules().stream()
                .map(module -> new DecorationHomeModuleResponse(
                        module.moduleId(),
                        trimRequired(module.moduleType()),
                        trimRequired(module.moduleName()),
                        module.sortOrder() == null ? 0 : module.sortOrder(),
                        module.enabled(),
                        trimOptional(module.styleMode()),
                        trimOptional(module.dataSourceType()),
                        module.dataSourceId(),
                        module.jumpTarget(),
                        module.configPayload()))
                .toList();
        return new StoreDecorationConfigResponse(
                null,
                request.merchantId(),
                storeId,
                request.miniProgramId(),
                trimRequired(request.themeColor()),
                trimRequired(request.homeTemplateCode()),
                normalizeStyleMode(request.homeStyleMode()),
                request.headerServiceBar(),
                request.brandHeroBlock(),
                trimOptional(request.deliveryPromiseText()),
                trimOptional(request.deliveryFeeText()),
                trimOptional(request.minimumOrderText()),
                trimOptional(request.searchPlaceholder()),
                trimOptional(request.announcementText()),
                STATUS_DRAFT,
                0,
                modules);
    }

    private List<DecorationRepository.HomeModuleDraft> toModuleDrafts(List<DecorationHomeModuleRequest> modules) {
        return modules.stream()
                .map(module -> new DecorationRepository.HomeModuleDraft(
                        idGenerator.nextId(),
                        trimRequired(module.moduleType()),
                        trimRequired(module.moduleName()),
                        module.sortOrder() == null ? 0 : module.sortOrder(),
                        Boolean.TRUE.equals(module.enabled()),
                        trimOptional(module.styleMode()),
                        trimOptional(module.dataSourceType()),
                        module.dataSourceId(),
                        writeJson(module.jumpTarget()),
                        writeJson(module.configPayload())))
                .toList();
    }

    private List<DecorationRepository.HomeModuleDraft> toModuleDraftsFromResponse(List<DecorationHomeModuleResponse> modules) {
        return modules.stream()
                .map(module -> new DecorationRepository.HomeModuleDraft(
                        idGenerator.nextId(),
                        module.moduleType(),
                        module.moduleName(),
                        module.sortOrder() == null ? 0 : module.sortOrder(),
                        Boolean.TRUE.equals(module.enabled()),
                        module.styleMode(),
                        module.dataSourceType(),
                        module.dataSourceId(),
                        writeJson(module.jumpTarget()),
                        writeJson(module.configPayload())))
                .toList();
    }

    private StoreDecorationConfigResponse toResponse(DecorationRepository.HomeConfigRecord config,
                                                     List<DecorationRepository.HomeModuleRecord> modules) {
        List<DecorationHomeModuleResponse> moduleResponses = modules.stream()
                .map(module -> new DecorationHomeModuleResponse(
                        module.id(),
                        module.moduleType(),
                        module.moduleName(),
                        module.sortOrder(),
                        module.enabled(),
                        module.styleMode(),
                        module.dataSourceType(),
                        module.dataSourceId(),
                        readJson(module.jumpTargetJson()),
                        readJson(module.configPayloadJson())))
                .toList();
        return new StoreDecorationConfigResponse(
                config.id(),
                config.merchantId(),
                config.storeId(),
                config.miniProgramId(),
                config.themeColor(),
                config.homeTemplateCode(),
                config.homeStyleMode(),
                readJson(config.headerServiceBarJson()),
                readJson(config.brandHeroBlockJson()),
                config.deliveryPromiseText(),
                config.deliveryFeeText(),
                config.minimumOrderText(),
                config.searchPlaceholder(),
                config.announcementText(),
                config.status(),
                config.versionNo(),
                moduleResponses);
    }

    private void validateHomeStyleMode(String homeStyleMode) {
        String normalized = normalizeStyleMode(homeStyleMode);
        if (!"general_mall".equals(normalized)
                && !"fresh_retail".equals(normalized)
                && !"brand_retail".equals(normalized)) {
            throw new BusinessException(DecorationErrorCode.DECORATION_STYLE_MODE_INVALID);
        }
    }

    private void validateTemplateCode(String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            throw new BusinessException(DecorationErrorCode.DECORATION_TEMPLATE_CODE_INVALID);
        }
    }

    private String normalizeStyleMode(String value) {
        return trimRequired(value).toLowerCase(Locale.ROOT);
    }

    private String trimRequired(String value) {
        String trimmed = trimOptional(value);
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("必填字段不能为空");
        }
        return trimmed;
    }

    private String trimOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    private JsonNode readJson(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 解析失败", ex);
        }
    }

    private String writeSnapshot(StoreDecorationConfigResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("装修快照序列化失败", ex);
        }
    }

    private StoreDecorationConfigResponse readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, StoreDecorationConfigResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("装修快照解析失败", ex);
        }
    }
}
