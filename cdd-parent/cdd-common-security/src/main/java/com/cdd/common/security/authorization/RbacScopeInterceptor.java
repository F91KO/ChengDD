package com.cdd.common.security.authorization;

import com.cdd.common.security.context.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RbacScopeInterceptor implements HandlerInterceptor {

    private final RbacScopeAuthorizer authorizer;

    public RbacScopeInterceptor(RbacScopeAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        authorizer.authorize(
                handlerMethod.getMethod(),
                handlerMethod.getBeanType(),
                request,
                AuthContextHolder.get());
        return true;
    }
}
