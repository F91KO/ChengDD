package com.cdd.gateway.web;

import com.cdd.common.security.authorization.RequireAccountTypes;
import com.cdd.common.security.authorization.RequireRoles;
import com.cdd.common.security.authorization.RequireScope;
import com.cdd.gateway.config.GatewayRouteProperties;
import com.cdd.gateway.service.GatewayDownstreamClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayProxyController {

    private final GatewayDownstreamClient gatewayDownstreamClient;
    private final GatewayRouteProperties gatewayRouteProperties;

    public GatewayProxyController(GatewayDownstreamClient gatewayDownstreamClient,
                                  GatewayRouteProperties gatewayRouteProperties) {
        this.gatewayDownstreamClient = gatewayDownstreamClient;
        this.gatewayRouteProperties = gatewayRouteProperties;
    }

    @RequestMapping({
            "/api/auth/merchant/login",
            "/api/auth/platform/login",
            "/api/auth/token/refresh"
    })
    public ResponseEntity<byte[]> proxyAnonymousAuth(HttpServletRequest request,
                                                     @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getAuth().getBaseUrl());
    }

    @RequestMapping({
            "/api/auth/me",
            "/api/auth/logout"
    })
    @RequireAccountTypes({"platform", "merchant"})
    @RequireRoles(anyOf = {"platform_admin", "merchant_owner", "merchant_admin"})
    public ResponseEntity<byte[]> proxyProtectedAuth(HttpServletRequest request,
                                                     @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getAuth().getBaseUrl());
    }

    @RequestMapping("/api/report/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyReport(HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getReport().getBaseUrl());
    }

    @RequestMapping("/api/config/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyConfig(HttpServletRequest request,
                                              @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getConfig().getBaseUrl());
    }

    @RequestMapping("/api/product/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyProduct(HttpServletRequest request,
                                               @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getProduct().getBaseUrl());
    }

    @RequestMapping("/api/order/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyOrder(HttpServletRequest request,
                                             @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getOrder().getBaseUrl());
    }

    @RequestMapping("/api/release/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyRelease(HttpServletRequest request,
                                               @RequestBody(required = false) byte[] body) {
        return proxyTo(request, body, gatewayRouteProperties.getRelease().getBaseUrl());
    }

    @RequestMapping("/actuator/report/**")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> proxyReportActuator(HttpServletRequest request) {
        String query = request.getQueryString();
        String rewrittenPath = request.getRequestURI().replaceFirst("^/actuator/report", "/actuator");
        return gatewayDownstreamClient.get(
                gatewayRouteProperties.getReport().getBaseUrl(),
                rewrittenPath + (query == null || query.isBlank() ? "" : "?" + query),
                request);
    }

    private ResponseEntity<byte[]> proxyTo(HttpServletRequest request, byte[] body, String baseUrl) {
        return gatewayDownstreamClient.proxy(request, body, baseUrl);
    }
}
