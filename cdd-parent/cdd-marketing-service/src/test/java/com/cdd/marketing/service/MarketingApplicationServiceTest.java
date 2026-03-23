package com.cdd.marketing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cdd.api.marketing.model.ActivityProductRequest;
import com.cdd.api.marketing.model.MarketingActivityUpsertRequest;
import com.cdd.api.marketing.model.MarketingCouponUpsertRequest;
import com.cdd.api.marketing.model.MarketingRecommendRuleUpsertRequest;
import com.cdd.api.marketing.model.MarketingTopicFloorUpsertRequest;
import com.cdd.marketing.MarketingServiceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = MarketingServiceApplication.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MarketingApplicationServiceTest {

    private static final long MERCHANT_ID = 1001L;
    private static final long STORE_ID = 1001L;

    @Autowired
    private MarketingApplicationService marketingApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndListMarketingDomainData() throws Exception {
        var coupon = marketingApplicationService.createCoupon(new MarketingCouponUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "新人满减券",
                "cash",
                new BigDecimal("39.00"),
                new BigDecimal("10.00"),
                null,
                "2026-03-22 10:00:00",
                "2026-03-31 23:59:59",
                "enabled"));
        assertEquals("新人满减券", coupon.couponName());

        var activity = marketingApplicationService.createActivity(new MarketingActivityUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "春季鲜品会场",
                "promotion",
                "active",
                "2026-03-22 10:00:00",
                "2026-03-30 23:59:59",
                objectMapper.readTree("{\"tag\":\"spring-sale\"}"),
                "https://img.example.com/spring-sale.png",
                List.of(
                        new ActivityProductRequest(3100001L, 3200001L, new BigDecimal("49.90"), 10),
                        new ActivityProductRequest(3100002L, 3200002L, new BigDecimal("29.90"), 20))));
        assertEquals(2, activity.products().size());

        var rule = marketingApplicationService.createRecommendRule(new MarketingRecommendRuleUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "首页推荐流规则",
                "home_feed",
                objectMapper.readTree("{\"display_limit\":12,\"user_segments\":[\"new_user\",\"member\"]}"),
                "enabled"));
        assertEquals("home_feed", rule.sceneCode());

        var topic = marketingApplicationService.upsertTopicFloor(new MarketingTopicFloorUpsertRequest(
                MERCHANT_ID,
                STORE_ID,
                "春季专题馆",
                "spring_topic_01",
                "https://img.example.com/topic.png",
                objectMapper.readTree("{\"cards\":[{\"title\":\"应季上新\"}]}"),
                "enabled"));
        assertEquals("spring_topic_01", topic.topicCode());

        assertEquals(1, marketingApplicationService.listCoupons(MERCHANT_ID, STORE_ID, "enabled").size());
        assertEquals(1, marketingApplicationService.listActivities(MERCHANT_ID, STORE_ID, "active").size());
        assertEquals(1, marketingApplicationService.listRecommendRules(MERCHANT_ID, STORE_ID, "home_feed", "enabled").size());
        assertEquals(1, marketingApplicationService.listTopicFloors(MERCHANT_ID, STORE_ID, "enabled").size());
        assertTrue(marketingApplicationService.listActivities(MERCHANT_ID, STORE_ID, null).get(0).products().stream()
                .anyMatch(product -> product.productId().equals(3100001L)));
    }
}
