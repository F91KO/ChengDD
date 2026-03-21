package com.cdd.auth.service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RefreshTokenSessionStore {

    private final ConcurrentMap<String, RefreshTokenSession> sessionsByTokenId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> tokenIdsByUserId = new ConcurrentHashMap<>();

    public void save(RefreshTokenSession session) {
        sessionsByTokenId.put(session.tokenId(), session);
        tokenIdsByUserId.computeIfAbsent(session.userId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(session.tokenId());
    }

    public RefreshTokenSession find(String tokenId) {
        RefreshTokenSession session = sessionsByTokenId.get(tokenId);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            revoke(tokenId);
            return null;
        }
        return session;
    }

    public void revoke(String tokenId) {
        RefreshTokenSession session = sessionsByTokenId.remove(tokenId);
        if (session == null) {
            return;
        }
        Set<String> tokenIds = tokenIdsByUserId.get(session.userId());
        if (tokenIds == null) {
            return;
        }
        tokenIds.remove(tokenId);
        if (tokenIds.isEmpty()) {
            tokenIdsByUserId.remove(session.userId(), tokenIds);
        }
    }

    public void revokeAll(String userId) {
        Set<String> tokenIds = tokenIdsByUserId.remove(userId);
        if (tokenIds == null) {
            return;
        }
        for (String tokenId : tokenIds) {
            sessionsByTokenId.remove(tokenId);
        }
    }

    public record RefreshTokenSession(
            String tokenId,
            String userId,
            String refreshToken,
            long tokenVersion,
            Instant expiresAt) {
    }
}
