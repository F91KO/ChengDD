package com.cdd.auth.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthAccountRepository implements AuthAccountRepository {

    private static final RowMapper<AccountRow> ACCOUNT_ROW_MAPPER = JdbcAuthAccountRepository::mapAccountRow;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAuthAccountRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<StoredAccount> findByAccountName(String accountName) {
        List<AccountRow> rows = jdbcTemplate.query("""
                SELECT id, user_id, account_type, account_name, display_name, merchant_id, store_id,
                       mini_program_id, token_version, password_hash
                FROM cdd_auth_account
                WHERE account_name = ?
                  AND status = 'enabled'
                  AND deleted = 0
                """, ACCOUNT_ROW_MAPPER, accountName);
        return rows.stream().findFirst().map(this::toStoredAccount);
    }

    @Override
    public Optional<StoredAccount> findByUserId(String userId) {
        List<AccountRow> rows = jdbcTemplate.query("""
                SELECT id, user_id, account_type, account_name, display_name, merchant_id, store_id,
                       mini_program_id, token_version, password_hash
                FROM cdd_auth_account
                WHERE user_id = ?
                  AND status = 'enabled'
                  AND deleted = 0
                """, ACCOUNT_ROW_MAPPER, userId);
        return rows.stream().findFirst().map(this::toStoredAccount);
    }

    @Override
    public void updateLastLoginAt(long accountId, Instant lastLoginAt) {
        jdbcTemplate.update("""
                UPDATE cdd_auth_account
                SET last_login_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = 0
                """, Timestamp.from(lastLoginAt), accountId);
    }

    @Override
    public long incrementTokenVersion(String userId) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_auth_account
                SET token_version = token_version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND status = 'enabled'
                  AND deleted = 0
                """, userId);
        if (updated == 0) {
            return -1L;
        }
        Long tokenVersion = jdbcTemplate.queryForObject("""
                SELECT token_version
                FROM cdd_auth_account
                WHERE user_id = ?
                  AND status = 'enabled'
                  AND deleted = 0
                """, Long.class, userId);
        return tokenVersion == null ? -1L : tokenVersion;
    }

    private StoredAccount toStoredAccount(AccountRow row) {
        List<String> roleCodes = loadRoleCodes(row.accountId());
        return new StoredAccount(
                row.accountId(),
                row.userId(),
                row.accountType(),
                row.accountName(),
                row.displayName(),
                row.merchantId(),
                row.storeId(),
                row.miniProgramId(),
                roleCodes,
                loadPermissionModules(row.accountId(), roleCodes),
                loadActionPermissions(row.accountId(), roleCodes),
                row.tokenVersion(),
                row.passwordHash());
    }

    private List<String> loadRoleCodes(long accountId) {
        return jdbcTemplate.queryForList("""
                SELECT r.role_code
                FROM cdd_auth_account_role ar
                JOIN cdd_auth_role r ON r.id = ar.role_id
                WHERE ar.account_id = ?
                  AND ar.deleted = 0
                  AND r.deleted = 0
                  AND r.status = 'enabled'
                ORDER BY r.id ASC
                """, String.class, accountId);
    }

    private List<String> loadPermissionModules(long accountId, List<String> roleCodes) {
        if (!requiresMerchantSubAccountPermissions(roleCodes)) {
            return List.of();
        }
        return loadPermissionsJson(accountId, "permission_modules_json");
    }

    private List<String> loadActionPermissions(long accountId, List<String> roleCodes) {
        if (!requiresMerchantSubAccountPermissions(roleCodes)) {
            return List.of();
        }
        return loadPermissionsJson(accountId, "action_permissions_json");
    }

    private boolean requiresMerchantSubAccountPermissions(List<String> roleCodes) {
        boolean owner = roleCodes.stream().anyMatch(role -> "merchant_owner".equalsIgnoreCase(role));
        boolean admin = roleCodes.stream().anyMatch(role -> "merchant_admin".equalsIgnoreCase(role));
        return admin && !owner;
    }

    private List<String> loadPermissionsJson(long accountId, String columnName) {
        try {
            List<String> jsonValues = jdbcTemplate.query("""
                    SELECT %s
                    FROM cdd_merchant_sub_account
                    WHERE id = ?
                      AND deleted = 0
                    LIMIT 1
                    """.formatted(columnName), (rs, rowNum) -> rs.getString(columnName), accountId);
            if (jsonValues.isEmpty()) {
                return List.of();
            }
            String raw = jsonValues.get(0);
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (DataAccessException ex) {
            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static AccountRow mapAccountRow(ResultSet rs, int rowNum) throws SQLException {
        return new AccountRow(
                rs.getLong("id"),
                rs.getString("user_id"),
                rs.getString("account_type"),
                rs.getString("account_name"),
                rs.getString("display_name"),
                rs.getString("merchant_id"),
                rs.getString("store_id"),
                rs.getString("mini_program_id"),
                rs.getLong("token_version"),
                rs.getString("password_hash"));
    }

    private record AccountRow(
            long accountId,
            String userId,
            String accountType,
            String accountName,
            String displayName,
            String merchantId,
            String storeId,
            String miniProgramId,
            long tokenVersion,
            String passwordHash) {
    }
}
