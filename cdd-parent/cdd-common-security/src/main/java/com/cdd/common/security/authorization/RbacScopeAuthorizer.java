package com.cdd.common.security.authorization;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.security.config.CddSecurityProperties;
import com.cdd.common.security.context.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

public class RbacScopeAuthorizer {

    private final CddSecurityProperties securityProperties;

    public RbacScopeAuthorizer(CddSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void authorize(Method method, Class<?> targetType, HttpServletRequest request, AuthContext authContext) {
        RequireRoles requireRoles = resolveAnnotation(method, targetType, RequireRoles.class);
        RequireAccountTypes requireAccountTypes = resolveAnnotation(method, targetType, RequireAccountTypes.class);
        RequireScope requireScope = resolveAnnotation(method, targetType, RequireScope.class);
        if (requireRoles == null && requireAccountTypes == null && requireScope == null) {
            return;
        }
        if (authContext == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "未登录或登录已失效");
        }
        if (securityProperties.isRbacEnabled()) {
            authorizeRoles(requireRoles, authContext);
            authorizeAccountTypes(requireAccountTypes, authContext);
        }
        if (securityProperties.isScopeEnabled()) {
            authorizeScope(requireScope, request, authContext);
        }
    }

    private void authorizeRoles(RequireRoles requireRoles, AuthContext authContext) {
        if (requireRoles == null) {
            return;
        }
        Set<String> ownedRoles = authContext.getRoleCodes().stream()
                .map(RbacScopeAuthorizer::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> anyRoles = Arrays.stream(requireRoles.anyOf())
                .map(RbacScopeAuthorizer::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> allRoles = Arrays.stream(requireRoles.allOf())
                .map(RbacScopeAuthorizer::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!anyRoles.isEmpty() && ownedRoles.stream().noneMatch(anyRoles::contains)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：缺少所需角色");
        }
        if (!allRoles.isEmpty() && !ownedRoles.containsAll(allRoles)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：缺少完整角色集合");
        }
    }

    private void authorizeAccountTypes(RequireAccountTypes requireAccountTypes, AuthContext authContext) {
        if (requireAccountTypes == null) {
            return;
        }
        Set<String> allowedTypes = Arrays.stream(requireAccountTypes.value())
                .map(RbacScopeAuthorizer::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (allowedTypes.isEmpty()) {
            return;
        }
        String currentType = normalize(authContext.getAccountType());
        if (!allowedTypes.contains(currentType)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：账号类型不匹配");
        }
    }

    private void authorizeScope(RequireScope requireScope, HttpServletRequest request, AuthContext authContext) {
        if (requireScope == null) {
            return;
        }
        String merchantId = clean(authContext.getMerchantId());
        String storeId = clean(authContext.getStoreId());
        if (requireScope.requireMerchant() && !StringUtils.hasText(merchantId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：缺少商家数据范围");
        }
        if (requireScope.requireStore() && !StringUtils.hasText(storeId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：缺少店铺数据范围");
        }
        if (requireScope.enforceMerchantHeaderMatch()) {
            String headerMerchantId = clean(request.getHeader(RequestHeaders.MERCHANT_ID));
            if (StringUtils.hasText(headerMerchantId) && !StringUtils.hasText(merchantId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：账号无商家数据范围");
            }
            if (StringUtils.hasText(headerMerchantId) && !merchantId.equals(headerMerchantId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：商家数据范围不匹配");
            }
        }
        if (requireScope.enforceStoreHeaderMatch()) {
            String headerStoreId = clean(request.getHeader(RequestHeaders.STORE_ID));
            if (StringUtils.hasText(headerStoreId) && !StringUtils.hasText(storeId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：账号无店铺数据范围");
            }
            if (StringUtils.hasText(headerStoreId) && !storeId.equals(headerStoreId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN, "无权限访问：店铺数据范围不匹配");
            }
        }
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static <T extends Annotation> T resolveAnnotation(Method method, Class<?> targetType, Class<T> annotationType) {
        T methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(targetType, annotationType);
    }
}
