package com.cdd.gateway.web;

import com.cdd.common.security.authorization.RequireAccountTypes;
import com.cdd.common.security.authorization.RequireRoles;
import com.cdd.common.security.authorization.RequireScope;
import com.cdd.gateway.config.GatewayRouteProperties;
import com.cdd.gateway.service.GatewayDownstreamClient;
import com.cdd.gateway.service.GatewayRouteResolver;
import com.cdd.gateway.service.MerchantPermissionAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayProxyController {

    private final GatewayDownstreamClient gatewayDownstreamClient;
    private final GatewayRouteProperties gatewayRouteProperties;
    private final GatewayRouteResolver gatewayRouteResolver;
    private final MerchantPermissionAuthorizer merchantPermissionAuthorizer;

    public GatewayProxyController(GatewayDownstreamClient gatewayDownstreamClient,
                                  GatewayRouteProperties gatewayRouteProperties,
                                  GatewayRouteResolver gatewayRouteResolver,
                                  MerchantPermissionAuthorizer merchantPermissionAuthorizer) {
        this.gatewayDownstreamClient = gatewayDownstreamClient;
        this.gatewayRouteProperties = gatewayRouteProperties;
        this.gatewayRouteResolver = gatewayRouteResolver;
        this.merchantPermissionAuthorizer = merchantPermissionAuthorizer;
    }

    @RequestMapping({
            "/api/auth/merchant/login",
            "/api/auth/platform/login",
            "/api/auth/token/refresh"
    })
    public ResponseEntity<byte[]> proxyAnonymousAuth(HttpServletRequest request,
                                                     @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getAuth()));
    }

    @RequestMapping({
            "/api/auth/me",
            "/api/auth/logout"
    })
    @RequireAccountTypes({"platform", "merchant"})
    @RequireRoles(anyOf = {"platform_admin", "merchant_owner", "merchant_admin"})
    public ResponseEntity<byte[]> proxyProtectedAuth(HttpServletRequest request,
                                                     @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getAuth()));
    }

    @RequestMapping("/api/merchant/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true)
    public ResponseEntity<byte[]> proxyMerchant(HttpServletRequest request,
                                                @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getMerchant()));
    }

    @RequestMapping("/api/decoration/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyDecoration(HttpServletRequest request,
                                                  @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getDecoration()));
    }

    @RequestMapping("/api/report/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyReport(HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getReport()));
    }

    @RequestMapping("/api/config/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyConfig(HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getConfig()));
    }

    @RequestMapping("/api/product/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyProduct(HttpServletRequest request,
                                               @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getProduct()));
    }

    @RequestMapping("/api/order/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyOrder(HttpServletRequest request,
                                             @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getOrder()));
    }

    @RequestMapping("/api/marketing/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyMarketing(HttpServletRequest request,
                                                 @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getMarketing()));
    }

    @RequestMapping("/api/release/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyRelease(HttpServletRequest request,
                                               @RequestBody(required = false) byte[] body) {
        merchantPermissionAuthorizer.authorize(request);
        return proxyTo(request, body, gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getRelease()));
    }

    @RequestMapping("/actuator/report/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyReportActuator(HttpServletRequest request) {
        merchantPermissionAuthorizer.authorize(request);
        String query = request.getQueryString();
        String rewrittenPath = request.getRequestURI().replaceFirst("^/actuator/report", "/actuator");
        return gatewayDownstreamClient.get(
                gatewayRouteResolver.resolveBaseUrl(gatewayRouteProperties.getReport()),
                rewrittenPath + (query == null || query.isBlank() ? "" : "?" + query),
                request);
    }

    private ResponseEntity<byte[]> proxyTo(HttpServletRequest request, byte[] body, String baseUrl) {
        return gatewayDownstreamClient.proxy(request, body, baseUrl);
    }
}
