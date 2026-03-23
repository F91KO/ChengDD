package com.cdd.marketing.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public interface MarketingRepository {

    CouponRecord createCoupon(long id,
                              long merchantId,
                              long storeId,
                              String couponName,
                              String couponType,
                              BigDecimal thresholdAmount,
                              BigDecimal discountAmount,
                              BigDecimal discountRate,
                              Timestamp issueStartAt,
                              Timestamp issueEndAt,
                              String status);

    List<CouponRecord> listCoupons(long merchantId, long storeId, String status);

    ActivityRecord createActivity(long id,
                                  long merchantId,
                                  long storeId,
                                  String activityName,
                                  String activityType,
                                  String activityStatus,
                                  Timestamp startAt,
                                  Timestamp endAt,
                                  String rulePayloadJson,
                                  String bannerUrl);

    void createActivityProducts(long merchantId,
                                long storeId,
                                long activityId,
                                List<ActivityProductDraft> products);

    List<ActivityRecord> listActivities(long merchantId, long storeId, String activityStatus);

    List<ActivityProductRecord> listActivityProducts(long activityId);

    RecommendRuleRecord createRecommendRule(long id,
                                            long merchantId,
                                            long storeId,
                                            String ruleName,
                                            String sceneCode,
                                            String rulePayloadJson,
                                            String status);

    List<RecommendRuleRecord> listRecommendRules(long merchantId, long storeId, String sceneCode, String status);

    TopicFloorRecord upsertTopicFloor(long id,
                                      long merchantId,
                                      long storeId,
                                      String topicName,
                                      String topicCode,
                                      String bannerUrl,
                                      String floorPayloadJson,
                                      String status);

    List<TopicFloorRecord> listTopicFloors(long merchantId, long storeId, String status);

    record CouponRecord(
            long id,
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
    }

    record ActivityRecord(
            long id,
            long merchantId,
            long storeId,
            String activityName,
            String activityType,
            String activityStatus,
            Timestamp startAt,
            Timestamp endAt,
            String rulePayloadJson,
            String bannerUrl) {
    }

    record ActivityProductDraft(
            long id,
            long productId,
            Long skuId,
            BigDecimal activityPrice,
            int sortOrder) {
    }

    record ActivityProductRecord(
            long id,
            long productId,
            Long skuId,
            BigDecimal activityPrice,
            int sortOrder) {
    }

    record RecommendRuleRecord(
            long id,
            long merchantId,
            long storeId,
            String ruleName,
            String sceneCode,
            String rulePayloadJson,
            String status) {
    }

    record TopicFloorRecord(
            long id,
            long merchantId,
            long storeId,
            String topicName,
            String topicCode,
            String bannerUrl,
            String floorPayloadJson,
            String status) {
    }
}
