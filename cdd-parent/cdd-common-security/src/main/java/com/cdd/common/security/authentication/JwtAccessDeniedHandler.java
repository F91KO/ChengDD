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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;

public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = StringUtils.hasText(accessDeniedException.getMessage())
                ? accessDeniedException.getMessage()
                : CommonErrorCode.FORBIDDEN.getMessage();
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                CommonErrorCode.FORBIDDEN.getCode(),
                message,
                request.getHeader(RequestHeaders.REQUEST_ID)));
    }
}
