package com.cdd.decoration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cdd.api.decoration.model.DecorationHomeModuleRequest;
import com.cdd.api.decoration.model.PublishDecorationRequest;
import com.cdd.api.decoration.model.RollbackDecorationRequest;
import com.cdd.api.decoration.model.SaveDecorationDraftRequest;
import com.cdd.decoration.DecorationServiceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = DecorationServiceApplication.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DecorationApplicationServiceTest {

    private static final long MERCHANT_ID = 1001L;
    private static final long STORE_ID = 1001L;
    private static final long MINI_PROGRAM_ID = 1001L;

    @Autowired
    private DecorationApplicationService decorationApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveDraftAndPublishDecorationConfig() throws Exception {
        var draft = decorationApplicationService.saveDraft(STORE_ID, buildRequest(
                "#12AB34",
                "fresh-default",
                "fresh_retail",
                "最快 30 分钟送达"));

        assertEquals("draft", draft.status());
        assertEquals(0, draft.versionNo());
        assertEquals(2, draft.homeModules().size());

        var published = decorationApplicationService.publish(
                STORE_ID,
                new PublishDecorationRequest(MERCHANT_ID, MINI_PROGRAM_ID));

        assertEquals("published", published.status());
        assertEquals(1, published.versionNo());
        assertEquals("fresh-default", published.homeTemplateCode());
        assertTrue(published.homeModules().stream().anyMatch(module -> "banner".equals(module.moduleType())));

        var loaded = decorationApplicationService.getDecorationConfig(MERCHANT_ID, STORE_ID, MINI_PROGRAM_ID);
        assertEquals("published", loaded.status());
        assertEquals(1, loaded.versionNo());
        assertEquals("#12AB34", loaded.themeColor());
    }

    @Test
    void shouldRollbackToPreviousPublishedVersion() throws Exception {
        decorationApplicationService.saveDraft(STORE_ID, buildRequest(
                "#12AB34",
                "fresh-default",
                "fresh_retail",
                "最快 30 分钟送达"));
        decorationApplicationService.publish(STORE_ID, new PublishDecorationRequest(MERCHANT_ID, MINI_PROGRAM_ID));

        decorationApplicationService.saveDraft(STORE_ID, buildRequest(
                "#FF6600",
                "brand-default",
                "brand_retail",
                "品牌专属次日达"));
        var secondPublished = decorationApplicationService.publish(
                STORE_ID,
                new PublishDecorationRequest(MERCHANT_ID, MINI_PROGRAM_ID));

        assertEquals(2, secondPublished.versionNo());
        assertEquals("#FF6600", secondPublished.themeColor());

        var rollbacked = decorationApplicationService.rollback(
                STORE_ID,
                new RollbackDecorationRequest(MERCHANT_ID, MINI_PROGRAM_ID));

        assertEquals("rollbacked", rollbacked.status());
        assertEquals(3, rollbacked.versionNo());
        assertEquals("#12AB34", rollbacked.themeColor());
        assertEquals("fresh-default", rollbacked.homeTemplateCode());
    }

    private SaveDecorationDraftRequest buildRequest(String themeColor,
                                                    String templateCode,
                                                    String styleMode,
                                                    String promiseText) throws Exception {
        return new SaveDecorationDraftRequest(
                MERCHANT_ID,
                MINI_PROGRAM_ID,
                themeColor,
                templateCode,
                styleMode,
                objectMapper.readTree("""
                        {"store_display_name":"浦东旗舰店","delivery_time_text":"30分钟达"}
                        """),
                objectMapper.readTree("""
                        {"hero_title":"春季上新","hero_sub_title":"本周主推"}
                        """),
                promiseText,
                "满39元免配送费",
                "29元起送",
                "搜一搜春季鲜品",
                "今日首单立减 10 元",
                List.of(
                        new DecorationHomeModuleRequest(
                                null,
                                "banner",
                                "首页主 Banner",
                                10,
                                true,
                                "fresh_retail_large_card",
                                "manual",
                                null,
                                objectMapper.readTree("{\"path\":\"/activity/spring-sale\"}"),
                                objectMapper.readTree("{\"display_limit\":1}")),
                        new DecorationHomeModuleRequest(
                                null,
                                "product_floor",
                                "时令推荐",
                                20,
                                true,
                                "fresh_retail_double_column",
                                "product_group",
                                9001L,
                                objectMapper.readTree("{\"path\":\"/product/group/9001\"}"),
                                objectMapper.readTree("{\"display_limit\":6,\"show_add_to_cart\":true}"))));
    }
}
