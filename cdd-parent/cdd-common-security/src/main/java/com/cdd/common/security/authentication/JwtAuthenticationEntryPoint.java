package com.cdd.common.security.authentication;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = authException instanceof JwtAuthenticationException && StringUtils.hasText(authException.getMessage())
                ? authException.getMessage()
                : CommonErrorCode.UNAUTHORIZED.getMessage();
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                CommonErrorCode.UNAUTHORIZED.getCode(),
                message,
                request.getHeader(RequestHeaders.REQUEST_ID)));
    }
}
