package com.cdd.auth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportPlatformLoginRefreshAndLogout() throws Exception {
        JsonNode loginData = login("platform_admin", "admin123456");
        String accessToken = loginData.path("access_token").asText();
        String refreshToken = loginData.path("refresh_token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user_id").value("p_1001"))
                .andExpect(jsonPath("$.data.account_type").value("platform"))
                .andExpect(jsonPath("$.data.role_codes[0]").value("platform_admin"));

        JsonNode refreshData = refresh(refreshToken);
        String rotatedRefreshToken = refreshData.path("refresh_token").asText();
        assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("refresh_token", rotatedRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("refresh_token", rotatedRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40112))
                .andExpect(jsonPath("$.message").value("刷新令牌无效或已失效"));
    }

    @Test
    void shouldKeepMerchantStoreIdInCurrentContextWhenScopeHeadersAreAbsent() throws Exception {
        JsonNode loginData = merchantLogin("merchant_admin", "merchant123456");
        String accessToken = loginData.path("access_token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user_id").value("m_1001"))
                .andExpect(jsonPath("$.data.account_type").value("merchant"))
                .andExpect(jsonPath("$.data.merchant_id").value("merchant_1001"))
                .andExpect(jsonPath("$.data.store_id").value("store_1001"))
                .andExpect(jsonPath("$.data.role_codes[0]").value("merchant_owner"));
    }

    private JsonNode login(String accountName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/platform/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "account_name", accountName,
                                "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andReturn();
        return readData(result);
    }

    private JsonNode merchantLogin(String accountName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/merchant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "account_name", accountName,
                                "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andReturn();
        return readData(result);
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("refresh_token", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readData(result);
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String writeJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
