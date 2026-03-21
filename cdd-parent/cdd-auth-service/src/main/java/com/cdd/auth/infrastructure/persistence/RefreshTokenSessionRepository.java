package com.cdd.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenSessionRepository {

    void save(StoredRefreshTokenSession session);

    Optional<StoredRefreshTokenSession> findActiveByTokenId(String tokenId, Instant now);

    void revoke(String tokenId, Instant revokedAt);

    void revokeAll(String userId, Instant revokedAt);
}
