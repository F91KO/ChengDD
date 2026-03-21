package com.cdd.auth.service;

import com.cdd.auth.infrastructure.persistence.RefreshTokenSessionRepository;
import com.cdd.auth.infrastructure.persistence.StoredRefreshTokenSession;
import com.cdd.auth.support.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public class RefreshTokenSessionStore {

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final IdGenerator idGenerator;

    public RefreshTokenSessionStore(RefreshTokenSessionRepository refreshTokenSessionRepository,
                                    IdGenerator idGenerator) {
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.idGenerator = idGenerator;
    }

    public void save(String tokenId,
                     long accountId,
                     String userId,
                     String rawRefreshToken,
                     long tokenVersion,
                     Instant expiresAt) {
        refreshTokenSessionRepository.save(new StoredRefreshTokenSession(
                idGenerator.nextId(),
                tokenId,
                accountId,
                userId,
                hash(rawRefreshToken),
                tokenVersion,
                expiresAt));
    }

    public RefreshTokenSession find(String tokenId) {
        return refreshTokenSessionRepository.findActiveByTokenId(tokenId, Instant.now())
                .map(session -> new RefreshTokenSession(
                        session.tokenId(),
                        session.accountId(),
                        session.userId(),
                        session.tokenHash(),
                        session.tokenVersion(),
                        session.expiresAt()))
                .orElse(null);
    }

    public boolean matches(RefreshTokenSession session, String rawRefreshToken) {
        return session.tokenHash().equals(hash(rawRefreshToken));
    }

    public void revoke(String tokenId) {
        refreshTokenSessionRepository.revoke(tokenId, Instant.now());
    }

    public void revokeAll(String userId) {
        refreshTokenSessionRepository.revokeAll(userId, Instant.now());
    }

    private String hash(String rawRefreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    public record RefreshTokenSession(
            String tokenId,
            long accountId,
            String userId,
            String tokenHash,
            long tokenVersion,
            Instant expiresAt) {
    }
}
