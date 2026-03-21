package com.cdd.common.security.filter;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AuthContextHolder.set(new AuthContext(
                request.getHeader(RequestHeaders.AUTHORIZATION),
                request.getHeader(RequestHeaders.STORE_ID),
                request.getHeader(RequestHeaders.MINI_PROGRAM_ID)));
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
        }
    }
}
