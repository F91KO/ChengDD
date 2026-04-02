package com.cdd.gateway.web;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.security.authentication.JwtTokenService;
import com.cdd.common.security.context.AuthContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GatewayProxyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RestTemplate gatewayProxyRestTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(gatewayProxyRestTemplate)
                .ignoreExpectOrder(true)
                .build();
    }

    @Test
    void shouldProxyAuthenticatedReportRequestAndPropagateAuthContextHeaders() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://report-service.test/api/report/merchant-dashboard/latest?merchant_id=1001&store_id=1001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(header(RequestHeaders.USER_ID, "m_1001"))
                .andExpect(header(RequestHeaders.MERCHANT_ID, "merchant_1001"))
                .andExpect(header(RequestHeaders.STORE_ID, "store_1001"))
                .andExpect(header(RequestHeaders.PERMISSION_MODULES, "store,product,order,release,config"))
                .andExpect(header(RequestHeaders.ACTION_PERMISSIONS, "view,edit,publish,export"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"merchant_id":1001,"store_id":1001,"snapshot_time":"2026-03-31 09:00:00"}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/report/merchant-dashboard/latest")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.merchant_id").value(1001));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldExposeMerchantDashboardAggregationPath() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://report-service.test/api/report/merchant-dashboard/latest?merchant_id=1001&store_id=1001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"merchant_id":1001,"store_id":1001,"snapshot_time":"2026-03-31 09:00:00"}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/merchant/dashboard/latest")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.store_id").value(1001));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldExposeMerchantDashboardDailyTrendPaths() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://report-service.test/api/report/orders/daily?merchant_id=1001&store_id=1001&start_date=2026-03-25&end_date=2026-03-31"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":[{"stat_date":"2026-03-31","order_count":12}]}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/merchant/dashboard/orders/daily")
                        .queryParam("start_date", "2026-03-25")
                        .queryParam("end_date", "2026-03-31")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].order_count").value(12));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldExposeMerchantDashboardHealthPath() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://report-service.test/api/report/health?merchant_id=1001&store_id=1001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"ready":true,"summary":"ok","items":[]}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/merchant/dashboard/health")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ready").value(true));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldKeepAnonymousLoginAvailableThroughGateway() throws Exception {
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://auth-service.test/api/auth/merchant/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"access_token":"token-a","refresh_token":"token-b"}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/auth/merchant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account_name":"merchant_admin","password":"merchant123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.access_token").value("token-a"));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldProxyMerchantSubAccountRequestThroughGateway() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://merchant-service.test/api/merchant/accounts/sub-accounts?page=1&page_size=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(header(RequestHeaders.USER_ID, "m_1001"))
                .andExpect(header(RequestHeaders.MERCHANT_ID, "merchant_1001"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"list":[],"page":1,"page_size":20,"total":0}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/merchant/accounts/sub-accounts")
                        .queryParam("page", "1")
                        .queryParam("page_size", "20")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.total").value(0));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldProxyAuthenticatedCurrentAuthRequestThroughGateway() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://auth-service.test/api/auth/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(header(RequestHeaders.ACCOUNT_TYPE, "merchant"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"user_id":"m_1001","account_name":"merchant_admin","account_type":"merchant"}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account_type").value("merchant"));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldRequireAuthenticationForLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token":"token-b"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void shouldProxyReportActuatorThroughGateway() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://report-service.test/actuator/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"status":"UP"}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/actuator/report/health")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status":"UP"}
                        """));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldRejectProtectedProxyRequestWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/config/publish-records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void shouldRejectProtectedProxyRequestWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/config/publish-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void shouldRejectMerchantProxyRequestWhenRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/config/publish-records")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(new AuthContext(
                                null,
                                "m_1002",
                                "merchant_viewer",
                                "merchant viewer",
                                "merchant",
                                "merchant_1001",
                                "store_1001",
                                null,
                                List.of("merchant_viewer"),
                                List.of(),
                                List.of(),
                                1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void shouldRejectMerchantProxyRequestWhenAccountTypeIsForbidden() throws Exception {
        mockMvc.perform(get("/api/product/spu")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(new AuthContext(
                                null,
                                "p_1001",
                                "platform_operator",
                                "platform operator",
                                "platform",
                                "merchant_1001",
                                "store_1001",
                                null,
                                List.of("merchant_admin"),
                                List.of("product"),
                                List.of("view"),
                                1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void shouldRejectMerchantProxyRequestWhenStoreScopeIsMissing() throws Exception {
        mockMvc.perform(get("/api/order/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(new AuthContext(
                                null,
                                "m_1001",
                                "merchant_admin",
                                "merchant admin",
                                "merchant",
                                "merchant_1001",
                                null,
                                null,
                                List.of("merchant_admin"),
                                List.of("order"),
                                List.of("view"),
                                1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void shouldRejectPlatformDashboardRequestWhenAccountTypeIsForbidden() throws Exception {
        mockMvc.perform(get("/api/platform/dashboard/latest")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(new AuthContext(
                                null,
                                "m_1001",
                                "merchant_admin",
                                "merchant admin",
                                "merchant",
                                "merchant_1001",
                                "store_1001",
                                null,
                                List.of("platform_admin"),
                                List.of(),
                                List.of(),
                                1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void shouldRejectMerchantAdminWhenModulePermissionIsMissing() throws Exception {
        mockMvc.perform(get("/api/product/spu")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(new AuthContext(
                                null,
                                "ms_2001",
                                "sub_product_viewer",
                                "sub product viewer",
                                "merchant",
                                "merchant_1001",
                                "store_1001",
                                null,
                                List.of("merchant_admin"),
                                List.of("order"),
                                List.of("view"),
                                1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权限访问：缺少模块权限 product"));
    }

    @Test
    void shouldRejectMerchantAdminWhenActionPermissionIsMissing() throws Exception {
        mockMvc.perform(get("/api/order/orders/export")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(new AuthContext(
                                null,
                                "ms_2002",
                                "sub_order_viewer",
                                "sub order viewer",
                                "merchant",
                                "merchant_1001",
                                "store_1001",
                                null,
                                List.of("merchant_admin"),
                                List.of("order"),
                                List.of("view"),
                                1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权限访问：缺少动作权限 export"));
    }

    @Test
    void shouldPreserveDownstreamErrorStatusAndBody() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://config-service.test/api/config/publish-records?merchant_id=merchant_1001&store_id=store_1001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":50001,"message":"downstream failed","data":null}
                                """));

        mockMvc.perform(get("/api/config/publish-records")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {"code":50001,"message":"downstream failed","data":null}
                        """));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldProxyCsvDownloadHeaders() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://order-service.test/api/order/orders/export?merchant_id=1001&store_id=1001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("id,status\n1,paid\n", MediaType.parseMediaType("text/csv;charset=UTF-8"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders-export.csv\""));

        mockMvc.perform(get("/api/order/orders/export")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders-export.csv\""))
                .andExpect(content().string("id,status\n1,paid\n"));

        mockRestServiceServer.verify();
    }

    @Test
    void shouldInjectMerchantScopeIntoConfigPublishBodyFromToken() throws Exception {
        String bearerToken = bearerToken();
        mockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo("http://config-service.test/api/config/publish-records"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String json = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                    Assertions.assertTrue(json.contains("\"merchant_id\":\"merchant_1001\""));
                    Assertions.assertTrue(json.contains("\"store_id\":\"store_1001\""));
                    Assertions.assertTrue(json.contains("\"operator_name\":\"merchant admin\""));
                })
                .andRespond(withSuccess("""
                        {"code":0,"message":"success","data":{"task_no":"cfg_001","merchant_id":1001,"store_id":1001}}
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/config/publish-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .content("""
                                {"operator_name":"merchant admin","publish_note":"sync"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.task_no").value("cfg_001"));

        mockRestServiceServer.verify();
    }

    private String bearerToken() {
        return bearerToken(new AuthContext(
                null,
                "m_1001",
                "merchant_admin",
                "merchant admin",
                "merchant",
                "merchant_1001",
                "store_1001",
                null,
                List.of("merchant_admin"),
                List.of("store", "product", "order", "release", "config"),
                List.of("view", "edit", "publish", "export"),
                2L));
    }

    private String bearerToken(AuthContext authContext) {
        return "Bearer " + jwtTokenService.createAccessToken(authContext);
    }
}
