package com.cdd.marketing.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcMarketingRepository implements MarketingRepository {

    private static final RowMapper<CouponRecord> COUPON_ROW_MAPPER = JdbcMarketingRepository::mapCoupon;
    private static final RowMapper<ActivityRecord> ACTIVITY_ROW_MAPPER = JdbcMarketingRepository::mapActivity;
    private static final RowMapper<ActivityProductRecord> ACTIVITY_PRODUCT_ROW_MAPPER = JdbcMarketingRepository::mapActivityProduct;
    private static final RowMapper<RecommendRuleRecord> RECOMMEND_RULE_ROW_MAPPER = JdbcMarketingRepository::mapRecommendRule;
    private static final RowMapper<TopicFloorRecord> TOPIC_FLOOR_ROW_MAPPER = JdbcMarketingRepository::mapTopicFloor;

    private final JdbcTemplate jdbcTemplate;

    public JdbcMarketingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CouponRecord createCoupon(long id,
                                     long merchantId,
                                     long storeId,
                                     String couponName,
                                     String couponType,
                                     BigDecimal thresholdAmount,
                                     BigDecimal discountAmount,
                                     BigDecimal discountRate,
                                     Timestamp issueStartAt,
                                     Timestamp issueEndAt,
                                     String status) {
        jdbcTemplate.update("""
                INSERT INTO cdd_marketing_coupon
                (id, merchant_id, store_id, coupon_name, coupon_type, threshold_amount, discount_amount,
                 discount_rate, issue_start_at, issue_end_at, status, created_by, updated_by, deleted, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, merchantId, storeId, couponName, couponType, thresholdAmount, discountAmount, discountRate,
                issueStartAt, issueEndAt, status, merchantId, merchantId);
        return new CouponRecord(id, merchantId, storeId, couponName, couponType, thresholdAmount, discountAmount, discountRate,
                issueStartAt, issueEndAt, status);
    }

    @Override
    public List<CouponRecord> listCoupons(long merchantId, long storeId, String status) {
        if (!StringUtils.hasText(status)) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, store_id, coupon_name, coupon_type, threshold_amount, discount_amount,
                           discount_rate, issue_start_at, issue_end_at, status
                    FROM cdd_marketing_coupon
                    WHERE merchant_id = ?
                      AND store_id = ?
                      AND deleted = 0
                    ORDER BY id ASC
                    """, COUPON_ROW_MAPPER, merchantId, storeId);
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, coupon_name, coupon_type, threshold_amount, discount_amount,
                       discount_rate, issue_start_at, issue_end_at, status
                FROM cdd_marketing_coupon
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND status = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, COUPON_ROW_MAPPER, merchantId, storeId, status);
    }

    @Override
    public ActivityRecord createActivity(long id,
                                         long merchantId,
                                         long storeId,
                                         String activityName,
                                         String activityType,
                                         String activityStatus,
                                         Timestamp startAt,
                                         Timestamp endAt,
                                         String rulePayloadJson,
                                         String bannerUrl) {
        jdbcTemplate.update("""
                INSERT INTO cdd_marketing_activity
                (id, merchant_id, store_id, activity_name, activity_type, activity_status, start_at, end_at,
                 rule_payload_json, banner_url, created_by, updated_by, deleted, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, merchantId, storeId, activityName, activityType, activityStatus, startAt, endAt, rulePayloadJson,
                bannerUrl, merchantId, merchantId);
        return new ActivityRecord(id, merchantId, storeId, activityName, activityType, activityStatus, startAt, endAt, rulePayloadJson, bannerUrl);
    }

    @Override
    public void createActivityProducts(long merchantId,
                                       long storeId,
                                       long activityId,
                                       List<ActivityProductDraft> products) {
        for (ActivityProductDraft product : products) {
            jdbcTemplate.update("""
                    INSERT INTO cdd_marketing_activity_product
                    (id, activity_id, product_id, sku_id, activity_price, sort_order, created_by, updated_by, deleted, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                    """,
                    product.id(), activityId, product.productId(), product.skuId(), product.activityPrice(), product.sortOrder(),
                    merchantId, merchantId);
        }
    }

    @Override
    public List<ActivityRecord> listActivities(long merchantId, long storeId, String activityStatus) {
        if (!StringUtils.hasText(activityStatus)) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, store_id, activity_name, activity_type, activity_status, start_at, end_at,
                           rule_payload_json, banner_url
                    FROM cdd_marketing_activity
                    WHERE merchant_id = ?
                      AND store_id = ?
                      AND deleted = 0
                    ORDER BY id ASC
                    """, ACTIVITY_ROW_MAPPER, merchantId, storeId);
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, activity_name, activity_type, activity_status, start_at, end_at,
                       rule_payload_json, banner_url
                FROM cdd_marketing_activity
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND activity_status = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, ACTIVITY_ROW_MAPPER, merchantId, storeId, activityStatus);
    }

    @Override
    public List<ActivityProductRecord> listActivityProducts(long activityId) {
        return jdbcTemplate.query("""
                SELECT id, product_id, sku_id, activity_price, sort_order
                FROM cdd_marketing_activity_product
                WHERE activity_id = ?
                  AND deleted = 0
                ORDER BY sort_order ASC, id ASC
                """, ACTIVITY_PRODUCT_ROW_MAPPER, activityId);
    }

    @Override
    public RecommendRuleRecord createRecommendRule(long id,
                                                   long merchantId,
                                                   long storeId,
                                                   String ruleName,
                                                   String sceneCode,
                                                   String rulePayloadJson,
                                                   String status) {
        jdbcTemplate.update("""
                INSERT INTO cdd_marketing_recommend_rule
                (id, merchant_id, store_id, rule_name, scene_code, rule_payload_json, status, created_by, updated_by, deleted, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, merchantId, storeId, ruleName, sceneCode, rulePayloadJson, status, merchantId, merchantId);
        return new RecommendRuleRecord(id, merchantId, storeId, ruleName, sceneCode, rulePayloadJson, status);
    }

    @Override
    public List<RecommendRuleRecord> listRecommendRules(long merchantId, long storeId, String sceneCode, String status) {
        if (!StringUtils.hasText(sceneCode) && !StringUtils.hasText(status)) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, store_id, rule_name, scene_code, rule_payload_json, status
                    FROM cdd_marketing_recommend_rule
                    WHERE merchant_id = ?
                      AND store_id = ?
                      AND deleted = 0
                    ORDER BY id ASC
                    """, RECOMMEND_RULE_ROW_MAPPER, merchantId, storeId);
        }
        String normalizedSceneCode = StringUtils.hasText(sceneCode) ? sceneCode : null;
        String normalizedStatus = StringUtils.hasText(status) ? status : null;
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, rule_name, scene_code, rule_payload_json, status
                FROM cdd_marketing_recommend_rule
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND (? IS NULL OR scene_code = ?)
                  AND (? IS NULL OR status = ?)
                  AND deleted = 0
                ORDER BY id ASC
                """, RECOMMEND_RULE_ROW_MAPPER,
                merchantId, storeId, normalizedSceneCode, normalizedSceneCode, normalizedStatus, normalizedStatus);
    }

    @Override
    public TopicFloorRecord upsertTopicFloor(long id,
                                             long merchantId,
                                             long storeId,
                                             String topicName,
                                             String topicCode,
                                             String bannerUrl,
                                             String floorPayloadJson,
                                             String status) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_marketing_topic_floor
                SET topic_name = ?, banner_url = ?, floor_payload_json = ?, status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND topic_code = ?
                  AND deleted = 0
                """, topicName, bannerUrl, floorPayloadJson, status, merchantId, merchantId, storeId, topicCode);
        if (updated == 0) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO cdd_marketing_topic_floor
                        (id, merchant_id, store_id, topic_name, topic_code, banner_url, floor_payload_json, status,
                         created_by, updated_by, deleted, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                        """,
                        id, merchantId, storeId, topicName, topicCode, bannerUrl, floorPayloadJson, status, merchantId, merchantId);
            } catch (DuplicateKeyException ex) {
                jdbcTemplate.update("""
                        UPDATE cdd_marketing_topic_floor
                        SET topic_name = ?, banner_url = ?, floor_payload_json = ?, status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE merchant_id = ?
                          AND store_id = ?
                          AND topic_code = ?
                          AND deleted = 0
                        """, topicName, bannerUrl, floorPayloadJson, status, merchantId, merchantId, storeId, topicCode);
            }
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, topic_name, topic_code, banner_url, floor_payload_json, status
                FROM cdd_marketing_topic_floor
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND topic_code = ?
                  AND deleted = 0
                LIMIT 1
                """, TOPIC_FLOOR_ROW_MAPPER, merchantId, storeId, topicCode).stream().findFirst().orElseThrow();
    }

    @Override
    public List<TopicFloorRecord> listTopicFloors(long merchantId, long storeId, String status) {
        if (!StringUtils.hasText(status)) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, store_id, topic_name, topic_code, banner_url, floor_payload_json, status
                    FROM cdd_marketing_topic_floor
                    WHERE merchant_id = ?
                      AND store_id = ?
                      AND deleted = 0
                    ORDER BY id ASC
                    """, TOPIC_FLOOR_ROW_MAPPER, merchantId, storeId);
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, topic_name, topic_code, banner_url, floor_payload_json, status
                FROM cdd_marketing_topic_floor
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND status = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, TOPIC_FLOOR_ROW_MAPPER, merchantId, storeId, status);
    }

    private static CouponRecord mapCoupon(ResultSet rs, int rowNum) throws SQLException {
        return new CouponRecord(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("coupon_name"),
                rs.getString("coupon_type"),
                rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("discount_rate"),
                rs.getTimestamp("issue_start_at"),
                rs.getTimestamp("issue_end_at"),
                rs.getString("status"));
    }

    private static ActivityRecord mapActivity(ResultSet rs, int rowNum) throws SQLException {
        return new ActivityRecord(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("activity_name"),
                rs.getString("activity_type"),
                rs.getString("activity_status"),
                rs.getTimestamp("start_at"),
                rs.getTimestamp("end_at"),
                rs.getString("rule_payload_json"),
                rs.getString("banner_url"));
    }

    private static ActivityProductRecord mapActivityProduct(ResultSet rs, int rowNum) throws SQLException {
        return new ActivityProductRecord(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getObject("sku_id", Long.class),
                rs.getBigDecimal("activity_price"),
                rs.getInt("sort_order"));
    }

    private static RecommendRuleRecord mapRecommendRule(ResultSet rs, int rowNum) throws SQLException {
        return new RecommendRuleRecord(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("rule_name"),
                rs.getString("scene_code"),
                rs.getString("rule_payload_json"),
                rs.getString("status"));
    }

    private static TopicFloorRecord mapTopicFloor(ResultSet rs, int rowNum) throws SQLException {
        return new TopicFloorRecord(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("topic_name"),
                rs.getString("topic_code"),
                rs.getString("banner_url"),
                rs.getString("floor_payload_json"),
                rs.getString("status"));
    }
}
