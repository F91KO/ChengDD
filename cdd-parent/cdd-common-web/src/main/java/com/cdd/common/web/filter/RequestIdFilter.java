package com.cdd.common.web.filter;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.web.context.RequestIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(RequestHeaders.REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        }

        RequestIdHolder.set(requestId);
        response.setHeader(RequestHeaders.REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestIdHolder.clear();
        }
    }
}
