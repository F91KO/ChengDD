package com.cdd.marketing.service;

import com.cdd.api.marketing.model.ActivityProductRequest;
import com.cdd.api.marketing.model.ActivityProductResponse;
import com.cdd.api.marketing.model.MarketingActivityResponse;
import com.cdd.api.marketing.model.MarketingActivityUpsertRequest;
import com.cdd.api.marketing.model.MarketingCouponResponse;
import com.cdd.api.marketing.model.MarketingCouponUpsertRequest;
import com.cdd.api.marketing.model.MarketingRecommendRuleResponse;
import com.cdd.api.marketing.model.MarketingRecommendRuleUpsertRequest;
import com.cdd.api.marketing.model.MarketingTopicFloorResponse;
import com.cdd.api.marketing.model.MarketingTopicFloorUpsertRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.marketing.error.MarketingErrorCode;
import com.cdd.marketing.infrastructure.persistence.MarketingRepository;
import com.cdd.marketing.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MarketingApplicationService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_UPCOMING = "upcoming";
    private static final String STATUS_FINISHED = "finished";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MarketingRepository marketingRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public MarketingApplicationService(MarketingRepository marketingRepository,
                                       IdGenerator idGenerator,
                                       ObjectMapper objectMapper) {
        this.marketingRepository = marketingRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MarketingCouponResponse createCoupon(MarketingCouponUpsertRequest request) {
        String status = normalizeCouponStatus(request.status());
        MarketingRepository.CouponRecord record = marketingRepository.createCoupon(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                trimRequired(request.couponName()),
                trimRequired(request.couponType()),
                request.thresholdAmount(),
                request.discountAmount(),
                request.discountRate(),
                parseDateTime(request.issueStartAt()),
                parseDateTime(request.issueEndAt()),
                status);
        return toCouponResponse(record);
    }

    public List<MarketingCouponResponse> listCoupons(long merchantId, long storeId, String status) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeCouponStatus(status) : null;
        return marketingRepository.listCoupons(merchantId, storeId, normalizedStatus).stream()
                .map(this::toCouponResponse)
                .toList();
    }

    @Transactional
    public MarketingActivityResponse createActivity(MarketingActivityUpsertRequest request) {
        if (request.products() == null || request.products().isEmpty()) {
            throw new BusinessException(MarketingErrorCode.MARKETING_ACTIVITY_PRODUCTS_REQUIRED);
        }
        String status = normalizeActivityStatus(request.activityStatus());
        long activityId = idGenerator.nextId();
        MarketingRepository.ActivityRecord record = marketingRepository.createActivity(
                activityId,
                request.merchantId(),
                request.storeId(),
                trimRequired(request.activityName()),
                trimRequired(request.activityType()),
                status,
                parseDateTime(request.startAt()),
                parseDateTime(request.endAt()),
                writeJson(request.rulePayload()),
                trimOptional(request.bannerUrl()));
        marketingRepository.createActivityProducts(
                request.merchantId(),
                request.storeId(),
                activityId,
                request.products().stream()
                        .map(product -> new MarketingRepository.ActivityProductDraft(
                                idGenerator.nextId(),
                                product.productId(),
                                product.skuId(),
                                product.activityPrice(),
                                product.sortOrder() == null ? 0 : product.sortOrder()))
                        .toList());
        return toActivityResponse(record, marketingRepository.listActivityProducts(activityId));
    }

    public List<MarketingActivityResponse> listActivities(long merchantId, long storeId, String status) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeActivityStatus(status) : null;
        return marketingRepository.listActivities(merchantId, storeId, normalizedStatus).stream()
                .map(record -> toActivityResponse(record, marketingRepository.listActivityProducts(record.id())))
                .toList();
    }

    @Transactional
    public MarketingRecommendRuleResponse createRecommendRule(MarketingRecommendRuleUpsertRequest request) {
        String status = normalizeRuleStatus(request.status());
        MarketingRepository.RecommendRuleRecord record = marketingRepository.createRecommendRule(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                trimRequired(request.ruleName()),
                trimRequired(request.sceneCode()),
                writeJson(request.rulePayload()),
                status);
        return toRecommendRuleResponse(record);
    }

    public List<MarketingRecommendRuleResponse> listRecommendRules(long merchantId, long storeId, String sceneCode, String status) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeRuleStatus(status) : null;
        return marketingRepository.listRecommendRules(merchantId, storeId, trimOptional(sceneCode), normalizedStatus).stream()
                .map(this::toRecommendRuleResponse)
                .toList();
    }

    @Transactional
    public MarketingTopicFloorResponse upsertTopicFloor(MarketingTopicFloorUpsertRequest request) {
        String status = normalizeTopicStatus(request.status());
        MarketingRepository.TopicFloorRecord record = marketingRepository.upsertTopicFloor(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                trimRequired(request.topicName()),
                trimRequired(request.topicCode()),
                trimOptional(request.bannerUrl()),
                writeJson(request.floorPayload()),
                status);
        return toTopicFloorResponse(record);
    }

    public List<MarketingTopicFloorResponse> listTopicFloors(long merchantId, long storeId, String status) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeTopicStatus(status) : null;
        return marketingRepository.listTopicFloors(merchantId, storeId, normalizedStatus).stream()
                .map(this::toTopicFloorResponse)
                .toList();
    }

    private MarketingCouponResponse toCouponResponse(MarketingRepository.CouponRecord record) {
        return new MarketingCouponResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                record.couponName(),
                record.couponType(),
                record.thresholdAmount(),
                record.discountAmount(),
                record.discountRate(),
                formatDateTime(record.issueStartAt()),
                formatDateTime(record.issueEndAt()),
                record.status());
    }

    private MarketingActivityResponse toActivityResponse(MarketingRepository.ActivityRecord record,
                                                         List<MarketingRepository.ActivityProductRecord> products) {
        return new MarketingActivityResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                record.activityName(),
                record.activityType(),
                record.activityStatus(),
                formatDateTime(record.startAt()),
                formatDateTime(record.endAt()),
                readJson(record.rulePayloadJson()),
                record.bannerUrl(),
                products.stream()
                        .map(product -> new ActivityProductResponse(
                                product.id(),
                                product.productId(),
                                product.skuId(),
                                product.activityPrice(),
                                product.sortOrder()))
                        .toList());
    }

    private MarketingRecommendRuleResponse toRecommendRuleResponse(MarketingRepository.RecommendRuleRecord record) {
        return new MarketingRecommendRuleResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                record.ruleName(),
                record.sceneCode(),
                readJson(record.rulePayloadJson()),
                record.status());
    }

    private MarketingTopicFloorResponse toTopicFloorResponse(MarketingRepository.TopicFloorRecord record) {
        return new MarketingTopicFloorResponse(
                record.id(),
                record.merchantId(),
                record.storeId(),
                record.topicName(),
                record.topicCode(),
                record.bannerUrl(),
                readJson(record.floorPayloadJson()),
                record.status());
    }

    private String normalizeCouponStatus(String status) {
        String normalized = trimRequired(status).toLowerCase(Locale.ROOT);
        if (!STATUS_DRAFT.equals(normalized) && !STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException(MarketingErrorCode.MARKETING_COUPON_STATUS_INVALID);
        }
        return normalized;
    }

    private String normalizeActivityStatus(String status) {
        String normalized = trimRequired(status).toLowerCase(Locale.ROOT);
        if (!STATUS_DRAFT.equals(normalized) && !STATUS_ACTIVE.equals(normalized)
                && !STATUS_UPCOMING.equals(normalized) && !STATUS_FINISHED.equals(normalized)
                && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException(MarketingErrorCode.MARKETING_ACTIVITY_STATUS_INVALID);
        }
        return normalized;
    }

    private String normalizeRuleStatus(String status) {
        String normalized = trimRequired(status).toLowerCase(Locale.ROOT);
        if (!STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized) && !STATUS_DRAFT.equals(normalized)) {
            throw new BusinessException(MarketingErrorCode.MARKETING_RULE_STATUS_INVALID);
        }
        return normalized;
    }

    private String normalizeTopicStatus(String status) {
        String normalized = trimRequired(status).toLowerCase(Locale.ROOT);
        if (!STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized) && !STATUS_DRAFT.equals(normalized)) {
            throw new BusinessException(MarketingErrorCode.MARKETING_TOPIC_STATUS_INVALID);
        }
        return normalized;
    }

    private Timestamp parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(value.trim(), DATETIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("日期时间格式必须为 yyyy-MM-dd HH:mm:ss", ex);
        }
    }

    private String formatDateTime(Timestamp value) {
        if (value == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(value.toLocalDateTime());
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
}
