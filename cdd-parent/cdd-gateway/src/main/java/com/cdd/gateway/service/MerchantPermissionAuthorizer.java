package com.cdd.gateway.service;

import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantPermissionAuthorizer {

    public void authorize(HttpServletRequest request) {
        AuthContext authContext = AuthContextHolder.get();
        if (authContext == null || !"merchant".equalsIgnoreCase(authContext.getAccountType())) {
            return;
        }
        if (hasRole(authContext, "merchant_owner") || !hasRole(authContext, "merchant_admin")) {
            return;
        }

        String module = requiredModule(request.getRequestURI());
        if (module == null) {
            return;
        }
        if (!contains(authContext.getPermissionModules(), module)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：缺少模块权限 " + module);
        }

        String action = requiredAction(request);
        if (action != null && !contains(authContext.getActionPermissions(), action)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：缺少动作权限 " + action);
        }
    }

    private boolean hasRole(AuthContext authContext, String roleCode) {
        return contains(authContext.getRoleCodes(), roleCode);
    }

    private boolean contains(List<String> values, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        Set<String> normalized = values.stream()
                .map(MerchantPermissionAuthorizer::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return normalized.contains(normalize(expected));
    }

    private String requiredModule(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        if (path.startsWith("/api/merchant/dashboard/")
                || path.startsWith("/api/report/")
                || path.startsWith("/actuator/report/")
                || path.startsWith("/api/decoration/")
                || path.startsWith("/api/marketing/")) {
            return "store";
        }
        if (path.startsWith("/api/product/")) {
            return "product";
        }
        if (path.startsWith("/api/order/")) {
            return "order";
        }
        if (path.startsWith("/api/config/")) {
            return "config";
        }
        if (path.startsWith("/api/release/")) {
            return "release";
        }
        return null;
    }

    private String requiredAction(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method)) {
            return path != null && path.contains("/export") ? "export" : "view";
        }
        if (path == null) {
            return "edit";
        }
        if (path.startsWith("/api/release/")
                || path.startsWith("/api/config/publish-records")
                || path.endsWith("/publish")
                || path.endsWith("/unpublish")
                || path.endsWith("/rollback")
                || path.endsWith("/sync")) {
            return "publish";
        }
        return "edit";
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
