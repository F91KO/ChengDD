package com.cdd.auth.infrastructure.persistence;

import java.time.Instant;

public record StoredRefreshTokenSession(
        long id,
        String tokenId,
        long accountId,
        String userId,
        String tokenHash,
        long tokenVersion,
        Instant expiresAt) {
}
