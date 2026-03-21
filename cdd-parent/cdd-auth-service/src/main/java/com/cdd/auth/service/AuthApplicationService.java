package com.cdd.auth.service;

import com.cdd.api.auth.model.CurrentAuthContextResponse;
import com.cdd.api.auth.model.LoginRequest;
import com.cdd.api.auth.model.LogoutRequest;
import com.cdd.api.auth.model.RefreshTokenRequest;
import com.cdd.api.auth.model.TokenResponse;
import com.cdd.auth.error.AuthErrorCode;
import com.cdd.auth.service.RefreshTokenSessionStore.RefreshTokenSession;
import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.security.authentication.JwtParsedToken;
import com.cdd.common.security.authentication.JwtTokenService;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final RuntimeAccountService runtimeAccountService;
    private final RefreshTokenSessionStore refreshTokenSessionStore;
    private final JwtTokenService jwtTokenService;

    public AuthApplicationService(RuntimeAccountService runtimeAccountService,
                                  RefreshTokenSessionStore refreshTokenSessionStore,
                                  JwtTokenService jwtTokenService) {
        this.runtimeAccountService = runtimeAccountService;
        this.refreshTokenSessionStore = refreshTokenSessionStore;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public TokenResponse login(String expectedAccountType, LoginRequest request) {
        AuthenticatedAccount account = runtimeAccountService.authenticate(expectedAccountType, request.accountName(), request.password());
        return issueTokens(account);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        JwtParsedToken refreshToken = parseRefreshToken(request.refreshToken());
        RefreshTokenSession session = requireRefreshSession(refreshToken, request.refreshToken());
        AuthenticatedAccount account = runtimeAccountService.getRequiredByUserId(refreshToken.authContext().getUserId());
        if (account.tokenVersion() != refreshToken.authContext().getTokenVersion()
                || session.tokenVersion() != account.tokenVersion()) {
            refreshTokenSessionStore.revoke(refreshToken.tokenId());
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        refreshTokenSessionStore.revoke(refreshToken.tokenId());
        return issueTokens(account);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        JwtParsedToken refreshToken = parseRefreshToken(request.refreshToken());
        requireRefreshSession(refreshToken, request.refreshToken());
        runtimeAccountService.incrementTokenVersion(refreshToken.authContext().getUserId());
        refreshTokenSessionStore.revokeAll(refreshToken.authContext().getUserId());
    }

    public CurrentAuthContextResponse current() {
        AuthContext authContext = AuthContextHolder.get();
        if (authContext == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return new CurrentAuthContextResponse(
                authContext.getUserId(),
                authContext.getAccountName(),
                authContext.getDisplayName(),
                authContext.getAccountType(),
                authContext.getMerchantId(),
                authContext.getStoreId(),
                authContext.getMiniProgramId(),
                authContext.getRoleCodes(),
                authContext.getTokenVersion());
    }

    private TokenResponse issueTokens(AuthenticatedAccount account) {
        AuthContext authContext = account.toAuthContext();
        String accessToken = jwtTokenService.createAccessToken(authContext);
        String refreshToken = jwtTokenService.createRefreshToken(authContext);
        JwtParsedToken parsedAccessToken = jwtTokenService.parseAccessToken(accessToken);
        JwtParsedToken parsedRefreshToken = jwtTokenService.parseRefreshToken(refreshToken);
        refreshTokenSessionStore.save(
                parsedRefreshToken.tokenId(),
                account.accountId(),
                authContext.getUserId(),
                refreshToken,
                authContext.getTokenVersion(),
                parsedRefreshToken.expiresAt());
        return new TokenResponse(
                RequestHeaders.BEARER_PREFIX.trim(),
                authContext.getAccountType(),
                accessToken,
                refreshToken,
                parsedAccessToken.expiresAt(),
                parsedRefreshToken.expiresAt());
    }

    private RefreshTokenSession requireRefreshSession(JwtParsedToken refreshToken, String rawRefreshToken) {
        RefreshTokenSession session = refreshTokenSessionStore.find(refreshToken.tokenId());
        if (session == null || !refreshTokenSessionStore.matches(session, rawRefreshToken)) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        return session;
    }

    private JwtParsedToken parseRefreshToken(String refreshToken) {
        try {
            return jwtTokenService.parseRefreshToken(refreshToken);
        } catch (AuthenticationException ex) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, ex.getMessage());
        }
    }
}
