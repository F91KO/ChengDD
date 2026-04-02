package com.cdd.gateway.web;

import com.cdd.common.security.authentication.JwtTokenService;
import com.cdd.common.security.context.AuthContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GatewayContextControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void shouldReturnCurrentContextWhenTokenIsValid() throws Exception {
        String accessToken = jwtTokenService.createAccessToken(new AuthContext(
                null,
                "m_1001",
                "merchant_admin",
                "商家管理员",
                "merchant",
                "merchant_1001",
                "store_1001",
                null,
                List.of("merchant_admin"),
                List.of("order"),
                List.of("view", "export"),
                2L));

        mockMvc.perform(get("/api/gateway/context")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user_id").value("m_1001"))
                .andExpect(jsonPath("$.data.account_name").value("merchant_admin"))
                .andExpect(jsonPath("$.data.account_type").value("merchant"))
                .andExpect(jsonPath("$.data.token_version").value(2))
                .andExpect(jsonPath("$.data.role_codes[0]").value("merchant_admin"))
                .andExpect(jsonPath("$.data.permission_modules[0]").value("order"))
                .andExpect(jsonPath("$.data.action_permissions[1]").value("export"));
    }

    @Test
    void shouldRejectWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/gateway/context"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("未登录或登录已失效"));
    }

    @Test
    void shouldRejectWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/gateway/context")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }
}
