package com.cdd.auth.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRefreshTokenSessionRepository implements RefreshTokenSessionRepository {

    private static final RowMapper<StoredRefreshTokenSession> SESSION_ROW_MAPPER = JdbcRefreshTokenSessionRepository::mapSession;

    private final JdbcTemplate jdbcTemplate;

    public JdbcRefreshTokenSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(StoredRefreshTokenSession session) {
        jdbcTemplate.update("""
                INSERT INTO cdd_auth_refresh_token_session
                (id, token_id, account_id, user_id, token_hash, token_version, expires_at, created_by, updated_by, deleted, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                session.id(),
                session.tokenId(),
                session.accountId(),
                session.userId(),
                session.tokenHash(),
                session.tokenVersion(),
                Timestamp.from(session.expiresAt()),
                0L,
                0L,
                0,
                0L);
    }

    @Override
    public Optional<StoredRefreshTokenSession> findActiveByTokenId(String tokenId, Instant now) {
        List<StoredRefreshTokenSession> sessions = jdbcTemplate.query("""
                SELECT id, token_id, account_id, user_id, token_hash, token_version, expires_at
                FROM cdd_auth_refresh_token_session
                WHERE token_id = ?
                  AND deleted = 0
                  AND revoked_at IS NULL
                  AND expires_at > ?
                """, SESSION_ROW_MAPPER, tokenId, Timestamp.from(now));
        return sessions.stream().findFirst();
    }

    @Override
    public void revoke(String tokenId, Instant revokedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_auth_refresh_token_session
                SET revoked_at = ?, deleted = 1, updated_at = CURRENT_TIMESTAMP
                WHERE token_id = ?
                  AND deleted = 0
                """, Timestamp.from(revokedAt), tokenId);
    }

    @Override
    public void revokeAll(String userId, Instant revokedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_auth_refresh_token_session
                SET revoked_at = ?, deleted = 1, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND deleted = 0
                """, Timestamp.from(revokedAt), userId);
    }

    private static StoredRefreshTokenSession mapSession(ResultSet rs, int rowNum) throws SQLException {
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        return new StoredRefreshTokenSession(
                rs.getLong("id"),
                rs.getString("token_id"),
                rs.getLong("account_id"),
                rs.getString("user_id"),
                rs.getString("token_hash"),
                rs.getLong("token_version"),
                expiresAt.toInstant());
    }
}
