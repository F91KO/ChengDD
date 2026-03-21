package com.cdd.config.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
}
