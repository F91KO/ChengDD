package com.cdd.common.security.authorization;

import com.cdd.common.core.error.BusinessException;
import com.cdd.common.security.config.CddSecurityProperties;
import com.cdd.common.security.context.AuthContext;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RbacScopeAuthorizerTest {

    private final RbacScopeAuthorizer authorizer = new RbacScopeAuthorizer(new CddSecurityProperties());

    @Test
    void shouldAllowWhenAnyRoleMatched() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("roleProtected");
        AuthContext authContext = context("platform", "merchant_1001", "store_1001", List.of("platform_admin"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        authorizer.authorize(method, DemoController.class, request, authContext);
    }

    @Test
    void shouldDenyWhenRoleMissing() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("roleProtected");
        AuthContext authContext = context("merchant", "merchant_1001", "store_1001", List.of("merchant_owner"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, authContext));
    }

    @Test
    void shouldDenyWhenAccountTypeNotMatched() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("platformOnly");
        AuthContext authContext = context("merchant", "merchant_1001", "store_1001", List.of("platform_admin"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, authContext));
    }

    @Test
    void shouldDenyWhenMerchantScopeRequiredButMissing() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("merchantScoped");
        AuthContext authContext = context("merchant", null, "store_1001", List.of("merchant_owner"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, authContext));
    }

    @Test
    void shouldDenyWhenStoreHeaderDoesNotMatch() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("merchantScoped");
        AuthContext authContext = context("merchant", "merchant_1001", "store_1001", List.of("merchant_owner"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Store-Id", "store_2002");
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, authContext));
    }

    @Test
    void shouldDenyWhenMerchantHeaderProvidedButAuthContextHasNoMerchantScope() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("merchantScoped");
        AuthContext authContext = context("platform", null, "store_1001", List.of("platform_admin"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Merchant-Id", "merchant_1001");
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, authContext));
    }

    @Test
    void shouldDenyWhenStoreHeaderProvidedButAuthContextHasNoStoreScope() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("merchantScoped");
        AuthContext authContext = context("merchant", "merchant_1001", null, List.of("merchant_owner"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Store-Id", "store_1001");
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, authContext));
    }

    @Test
    void shouldDenyWhenProtectedEndpointWithoutLogin() throws Exception {
        Method method = DemoController.class.getDeclaredMethod("roleProtected");
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                authorizer.authorize(method, DemoController.class, request, null));
    }

    private static AuthContext context(String accountType, String merchantId, String storeId, List<String> roleCodes) {
        return new AuthContext(
                "Bearer token",
                "u_1001",
                "tester",
                "测试账号",
                accountType,
                merchantId,
                storeId,
                null,
                roleCodes,
                0L);
    }

    private static class DemoController {

        @RequireRoles(anyOf = {"platform_admin"})
        void roleProtected() {
        }

        @RequireAccountTypes({"platform"})
        void platformOnly() {
        }

        @RequireScope(requireMerchant = true, requireStore = true)
        void merchantScoped() {
        }
    }
}
