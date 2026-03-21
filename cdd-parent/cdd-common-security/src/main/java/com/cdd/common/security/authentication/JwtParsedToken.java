package com.cdd.common.security.authentication;

import com.cdd.common.security.context.AuthContext;
import java.time.Instant;

public record JwtParsedToken(
        String tokenId,
        JwtTokenType tokenType,
        Instant issuedAt,
        Instant expiresAt,
        AuthContext authContext) {
}
