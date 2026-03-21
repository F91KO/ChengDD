package com.cdd.common.security.filter;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.security.authentication.JwtAuthenticationException;
import com.cdd.common.security.authentication.JwtParsedToken;
import com.cdd.common.security.authentication.JwtTokenService;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthContextFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public AuthContextFilter(JwtTokenService jwtTokenService, AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AuthContext authContext;
        try {
            authContext = resolveAuthContext(request);
        } catch (JwtAuthenticationException ex) {
            authenticationEntryPoint.commence(request, response, ex);
            return;
        }
        if (authContext != null) {
            AuthContextHolder.set(authContext);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    authContext.getUserId(),
                    null,
                    toAuthorities(authContext.getRoleCodes())));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private AuthContext resolveAuthContext(HttpServletRequest request) {
        String authorization = request.getHeader(RequestHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            if (!authorization.startsWith(RequestHeaders.BEARER_PREFIX)) {
                throw new JwtAuthenticationException("认证令牌格式错误");
            }
            String token = authorization.substring(RequestHeaders.BEARER_PREFIX.length());
            JwtParsedToken parsedToken = jwtTokenService.parseAccessToken(token);
            return parsedToken.authContext()
                    .withAuthorization(authorization)
                    .withScope(request.getHeader(RequestHeaders.STORE_ID), request.getHeader(RequestHeaders.MINI_PROGRAM_ID));
        }

        String userId = request.getHeader(RequestHeaders.USER_ID);
        if (!StringUtils.hasText(userId)) {
            return null;
        }

        return new AuthContext(
                authorization,
                userId,
                request.getHeader(RequestHeaders.ACCOUNT_NAME),
                request.getHeader(RequestHeaders.DISPLAY_NAME),
                request.getHeader(RequestHeaders.ACCOUNT_TYPE),
                request.getHeader(RequestHeaders.MERCHANT_ID),
                request.getHeader(RequestHeaders.STORE_ID),
                request.getHeader(RequestHeaders.MINI_PROGRAM_ID),
                splitRoleCodes(request.getHeader(RequestHeaders.ROLE_CODES)),
                parseTokenVersion(request.getHeader(RequestHeaders.TOKEN_VERSION)));
    }

    private List<GrantedAuthority> toAuthorities(List<String> roleCodes) {
        return roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private List<String> splitRoleCodes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private long parseTokenVersion(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
