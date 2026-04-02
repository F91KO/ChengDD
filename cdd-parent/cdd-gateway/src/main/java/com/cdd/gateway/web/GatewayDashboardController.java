package com.cdd.gateway.web;

import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.security.authorization.RequireAccountTypes;
import com.cdd.common.security.authorization.RequireRoles;
import com.cdd.common.security.authorization.RequireScope;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import com.cdd.gateway.config.GatewayRouteProperties;
import com.cdd.gateway.service.GatewayDownstreamClient;
import com.cdd.gateway.service.MerchantPermissionAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class GatewayDashboardController {

    private final GatewayDownstreamClient gatewayDownstreamClient;
    private final GatewayRouteProperties gatewayRouteProperties;
    private final MerchantPermissionAuthorizer merchantPermissionAuthorizer;

    public GatewayDashboardController(GatewayDownstreamClient gatewayDownstreamClient,
                                      GatewayRouteProperties gatewayRouteProperties,
                                      MerchantPermissionAuthorizer merchantPermissionAuthorizer) {
        this.gatewayDownstreamClient = gatewayDownstreamClient;
        this.gatewayRouteProperties = gatewayRouteProperties;
        this.merchantPermissionAuthorizer = merchantPermissionAuthorizer;
    }

    @GetMapping("/merchant/dashboard/latest")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> getMerchantDashboardLatest(HttpServletRequest request) {
        merchantPermissionAuthorizer.authorize(request);
        MerchantScope scope = resolveMerchantScope();
        return gatewayDownstreamClient.get(
                gatewayRouteProperties.getReport().getBaseUrl(),
                "/api/report/merchant-dashboard/latest?merchant_id=" + scope.merchantId() + "&store_id=" + scope.storeId(),
                request);
    }

    @GetMapping("/platform/dashboard/latest")
    @RequireAccountTypes({"platform"})
    @RequireRoles(anyOf = {"platform_admin"})
    public ResponseEntity<byte[]> getPlatformDashboardLatest(HttpServletRequest request) {
        return gatewayDownstreamClient.get(
                gatewayRouteProperties.getReport().getBaseUrl(),
                "/api/report/platform-dashboard/latest",
                request);
    }

    @GetMapping("/merchant/dashboard/orders/daily")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> getMerchantDashboardOrderDaily(@org.springframework.web.bind.annotation.RequestParam(value = "start_date", required = false) String startDate,
                                                                 @org.springframework.web.bind.annotation.RequestParam(value = "end_date", required = false) String endDate,
                                                                 HttpServletRequest request) {
        merchantPermissionAuthorizer.authorize(request);
        MerchantScope scope = resolveMerchantScope();
        return gatewayDownstreamClient.get(
                gatewayRouteProperties.getReport().getBaseUrl(),
                buildMerchantDashboardQueryPath("/api/report/orders/daily", scope.merchantId(), scope.storeId(), startDate, endDate),
                request);
    }

    @GetMapping("/merchant/dashboard/home-events/daily")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> getMerchantDashboardHomeEvents(@org.springframework.web.bind.annotation.RequestParam(value = "start_date", required = false) String startDate,
                                                                 @org.springframework.web.bind.annotation.RequestParam(value = "end_date", required = false) String endDate,
                                                                 HttpServletRequest request) {
        merchantPermissionAuthorizer.authorize(request);
        MerchantScope scope = resolveMerchantScope();
        return gatewayDownstreamClient.get(
                gatewayRouteProperties.getReport().getBaseUrl(),
                buildMerchantDashboardQueryPath("/api/report/home-events/daily", scope.merchantId(), scope.storeId(), startDate, endDate),
                request);
    }

    @GetMapping("/merchant/dashboard/health")
    @RequireAccountTypes({"merchant"})
    @RequireRoles(anyOf = {"merchant_owner", "merchant_admin"})
    @RequireScope(requireMerchant = true, requireStore = true)
    public ResponseEntity<byte[]> getMerchantDashboardHealth(HttpServletRequest request) {
        merchantPermissionAuthorizer.authorize(request);
        MerchantScope scope = resolveMerchantScope();
        return gatewayDownstreamClient.get(
                gatewayRouteProperties.getReport().getBaseUrl(),
                buildMerchantDashboardQueryPath("/api/report/health", scope.merchantId(), scope.storeId(), null, null),
                request);
    }

    private MerchantScope resolveMerchantScope() {
        AuthContext authContext = AuthContextHolder.get();
        if (authContext == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        Long merchantId = parseNumericTail(authContext.getMerchantId());
        Long storeId = parseNumericTail(authContext.getStoreId());
        if (merchantId == null || storeId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Merchant dashboard scope is missing");
        }
        return new MerchantScope(merchantId, storeId);
    }

    private Long parseNumericTail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        for (int index = raw.length() - 1; index >= 0; index--) {
            char current = raw.charAt(index);
            if (Character.isDigit(current)) {
                digits.insert(0, current);
                continue;
            }
            if (!digits.isEmpty()) {
                break;
            }
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildMerchantDashboardQueryPath(String path,
                                                   Long merchantId,
                                                   Long storeId,
                                                   String startDate,
                                                   String endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path)
                .queryParam("merchant_id", merchantId)
                .queryParam("store_id", storeId);
        if (startDate != null && !startDate.isBlank()) {
            builder.queryParam("start_date", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            builder.queryParam("end_date", endDate);
        }
        return builder.build().toUriString();
    }

    private record MerchantScope(Long merchantId, Long storeId) {
    }
}
