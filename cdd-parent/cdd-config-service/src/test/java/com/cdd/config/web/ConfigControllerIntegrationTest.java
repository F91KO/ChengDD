package com.cdd.config.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportConfigKvPlatformAndMerchantOverride() throws Exception {
        String configGroup = "checkout";
        String configKey = "timeout_seconds";
        String merchantId = "m10001";

        mockMvc.perform(post("/api/config/platform/kv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "config_group", configGroup,
                                "config_key", configKey,
                                "config_value", "30",
                                "config_desc", "下单超时时间（秒）"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.config_value").value("30"));

        mockMvc.perform(post("/api/config/platform/kv/merchant-overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", merchantId,
                                "config_group", configGroup,
                                "config_key", configKey,
                                "config_value", "45"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.source").value("merchant_override"));

        mockMvc.perform(get("/api/config/platform/kv/effective")
                        .param("merchant_id", merchantId)
                        .param("config_group", configGroup)
                        .param("config_key", configKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.config_value").value("45"))
                .andExpect(jsonPath("$.data.source").value("merchant_override"))
                .andExpect(jsonPath("$.data.merchant_id").value(merchantId));

        mockMvc.perform(get("/api/config/platform/kv/effective")
                        .param("merchant_id", "m10002")
                        .param("config_group", configGroup)
                        .param("config_key", configKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.config_value").value("30"))
                .andExpect(jsonPath("$.data.source").value("platform_default"));
    }

    @Test
    void shouldSupportFeatureSwitchMerchantOverrideAndDisabledFallback() throws Exception {
        String switchCode = "pay_refund_async";
        String merchantId = "m20001";

        mockMvc.perform(post("/api/config/platform/feature-switches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "switch_code", switchCode,
                                "switch_name", "异步退款开关",
                                "switch_scope", "merchant",
                                "default_value", "off",
                                "status", "enabled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.effective_value").value("off"));

        mockMvc.perform(post("/api/config/merchant/feature-switches/{switchCode}/change", switchCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", merchantId,
                                "switch_value", "on"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.effective_value").value("on"))
                .andExpect(jsonPath("$.data.source").value("merchant_override"));

        mockMvc.perform(get("/api/config/merchant/feature-switches/{switchCode}", switchCode)
                        .param("merchant_id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.effective_value").value("on"))
                .andExpect(jsonPath("$.data.source").value("merchant_override"));

        mockMvc.perform(post("/api/config/platform/feature-switches/{switchCode}/change-status", switchCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "disabled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("disabled"))
                .andExpect(jsonPath("$.data.effective_value").value("off"));

        mockMvc.perform(get("/api/config/merchant/feature-switches/{switchCode}", switchCode)
                        .param("merchant_id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("disabled"))
                .andExpect(jsonPath("$.data.effective_value").value("off"))
                .andExpect(jsonPath("$.data.source").value("platform_disabled"));
    }

    @Test
    void shouldReturnChineseMessageWhenFeatureSwitchValueInvalid() throws Exception {
        String switchCode = "config_invalid_value_test";

        mockMvc.perform(post("/api/config/platform/feature-switches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "switch_code", switchCode,
                                "switch_name", "值校验测试开关",
                                "switch_scope", "merchant",
                                "default_value", "off",
                                "status", "enabled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/config/merchant/feature-switches/{switchCode}/change", switchCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", "m30001",
                                "switch_value", "invalid_value"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40022))
                .andExpect(jsonPath("$.message").value("功能开关值仅支持 on 或 off"));
    }

    @Test
    void shouldSupportPublishRecordListDetailAndRollback() throws Exception {
        String merchantId = "merchant_910001";
        String storeId = "store_910001";
        String configGroup = "publish_test_group";
        String configKey = "publish_test_key";
        String switchCode = "publish_test_switch";

        mockMvc.perform(post("/api/config/platform/kv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "config_group", configGroup,
                                "config_key", configKey,
                                "config_value", "base-v1",
                                "config_desc", "发布测试配置"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.config_value").value("base-v1"));

        mockMvc.perform(post("/api/config/platform/kv/merchant-overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", merchantId,
                                "config_group", configGroup,
                                "config_key", configKey,
                                "config_value", "merchant-v1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.config_value").value("merchant-v1"));

        mockMvc.perform(post("/api/config/platform/feature-switches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "switch_code", switchCode,
                                "switch_name", "发布测试开关",
                                "switch_scope", "merchant",
                                "default_value", "off",
                                "status", "enabled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effective_value").value("off"));

        mockMvc.perform(post("/api/config/merchant/feature-switches/{switchCode}/change", switchCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", merchantId,
                                "switch_value", "on"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effective_value").value("on"));

        JsonNode publishRecordData = readData(mockMvc.perform(post("/api/config/publish-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", merchantId,
                                "store_id", storeId,
                                "operator_name", "配置管理员",
                                "publish_note", "一期联调发布"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.release_type").value("config_publish"))
                .andReturn());
        assertTrue(hasPlatformConfig(publishRecordData.path("snapshot").path("platform_configs"), configGroup, configKey, "base-v1"));
        assertTrue(hasMerchantFeatureSwitch(publishRecordData.path("snapshot").path("merchant_feature_switches"), switchCode, merchantId));
        String publishTaskNo = publishRecordData.path("task_no").asText();

        mockMvc.perform(get("/api/config/publish-records")
                        .param("merchant_id", merchantId)
                        .param("store_id", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].task_no").value(publishTaskNo));

        mockMvc.perform(get("/api/config/publish-records/{task_no}", publishTaskNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task_no").value(publishTaskNo))
                .andExpect(jsonPath("$.data.steps[0].step_code").value("config_snapshot_publish"));

        mockMvc.perform(post("/api/config/platform/kv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "config_group", configGroup,
                                "config_key", configKey,
                                "config_value", "base-v2",
                                "config_desc", "发布后修改"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/config/platform/kv/merchant-overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "merchant_id", merchantId,
                                "config_group", configGroup,
                                "config_key", configKey,
                                "config_value", "merchant-v2"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/config/platform/feature-switches/{switchCode}/change-status", switchCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "disabled"))))
                .andExpect(status().isOk());

        String rollbackTaskNo = readData(mockMvc.perform(post("/api/config/publish-records/{task_no}/rollback", publishTaskNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "operator_name", "配置管理员",
                                "rollback_reason", "回滚到稳定版本"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.release_type").value("config_rollback"))
                .andExpect(jsonPath("$.data.rollback_target_task_no").value(publishTaskNo))
                .andReturn()).path("task_no").asText();

        mockMvc.perform(get("/api/config/platform/kv/effective")
                        .param("merchant_id", merchantId)
                        .param("config_group", configGroup)
                        .param("config_key", configKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.config_value").value("merchant-v1"))
                .andExpect(jsonPath("$.data.source").value("merchant_override"));

        mockMvc.perform(get("/api/config/merchant/feature-switches/{switchCode}", switchCode)
                        .param("merchant_id", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effective_value").value("on"))
                .andExpect(jsonPath("$.data.source").value("merchant_override"));

        mockMvc.perform(get("/api/config/publish-records/{task_no}", publishTaskNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rollback_task_no").value(rollbackTaskNo));
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private boolean hasPlatformConfig(JsonNode configs, String configGroup, String configKey, String configValue) {
        for (JsonNode config : configs) {
            if (configGroup.equals(config.path("config_group").asText())
                    && configKey.equals(config.path("config_key").asText())
                    && configValue.equals(config.path("config_value").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMerchantFeatureSwitch(JsonNode switches, String switchCode, String merchantId) {
        for (JsonNode featureSwitch : switches) {
            if (switchCode.equals(featureSwitch.path("switch_code").asText())
                    && merchantId.equals(featureSwitch.path("merchant_id").asText())) {
                return true;
            }
        }
        return false;
    }
}
