package com.cdd.merchant.infrastructure.persistence;

import com.cdd.common.core.page.PageQuery;
import com.cdd.common.core.page.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcMerchantAccountRepository implements MerchantAccountRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final RowMapper<StoredMerchantSubAccount> SUB_ACCOUNT_ROW_MAPPER =
            JdbcMerchantAccountRepository::mapSubAccount;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcMerchantAccountRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<StoredMerchantSubAccount> pageSubAccounts(long merchantId, PageQuery pageQuery) {
        long total = Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_merchant_sub_account
                WHERE merchant_id = ?
                  AND deleted = 0
                """, Long.class, merchantId)).orElse(0L);
        List<StoredMerchantSubAccount> list = jdbcTemplate.query("""
                SELECT id, merchant_id, account_name, display_name, mobile, remark, status, role_label,
                       permission_modules_json, action_permissions_json, data_scope_type
                FROM cdd_merchant_sub_account
                WHERE merchant_id = ?
                  AND deleted = 0
                ORDER BY updated_at DESC, id DESC
                LIMIT ? OFFSET ?
                """, SUB_ACCOUNT_ROW_MAPPER, merchantId, pageQuery.pageSize(), (pageQuery.page() - 1L) * pageQuery.pageSize());
        return new PageResult<>(list, total);
    }

    @Override
    public Optional<StoredMerchantSubAccount> findSubAccount(long merchantId, long accountId) {
        List<StoredMerchantSubAccount> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, account_name, display_name, mobile, remark, status, role_label,
                       permission_modules_json, action_permissions_json, data_scope_type
                FROM cdd_merchant_sub_account
                WHERE merchant_id = ?
                  AND id = ?
                  AND deleted = 0
                """, SUB_ACCOUNT_ROW_MAPPER, merchantId, accountId);
        return rows.stream().findFirst();
    }

    @Override
    public void createSubAccount(StoredMerchantSubAccount subAccount) {
        jdbcTemplate.update("""
                INSERT INTO cdd_merchant_sub_account (
                  id, merchant_id, account_name, display_name, mobile, remark, status, role_label,
                  permission_modules_json, action_permissions_json, data_scope_type, password_hash, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?, ?, ?, ?)
                """,
                subAccount.accountId(),
                subAccount.merchantId(),
                subAccount.accountName(),
                subAccount.displayName(),
                subAccount.mobile(),
                subAccount.remark(),
                subAccount.status(),
                subAccount.roleLabel(),
                toJson(subAccount.permissionModules()),
                toJson(subAccount.actionPermissions()),
                subAccount.dataScopeType(),
                null,
                0L,
                0L);
    }

    @Override
    public void updateSubAccount(StoredMerchantSubAccount subAccount) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_sub_account
                SET account_name = ?,
                    display_name = ?,
                    mobile = ?,
                    remark = ?,
                    role_label = ?,
                    permission_modules_json = CAST(? AS JSON),
                    action_permissions_json = CAST(? AS JSON),
                    data_scope_type = ?,
                    updated_by = 0
                WHERE merchant_id = ?
                  AND id = ?
                  AND deleted = 0
                """,
                subAccount.accountName(),
                subAccount.displayName(),
                subAccount.mobile(),
                subAccount.remark(),
                subAccount.roleLabel(),
                toJson(subAccount.permissionModules()),
                toJson(subAccount.actionPermissions()),
                subAccount.dataScopeType(),
                subAccount.merchantId(),
                subAccount.accountId());
    }

    @Override
    public void disableSubAccount(long merchantId, long accountId, long operatorId) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_sub_account
                SET status = 'disabled',
                    updated_by = ?
                WHERE merchant_id = ?
                  AND id = ?
                  AND deleted = 0
                """, operatorId, merchantId, accountId);
    }

    @Override
    public void replaceScopes(long merchantId, long accountId, String scopeType, List<Long> scopeObjectIds, long operatorId) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_account_scope
                SET deleted = 1,
                    updated_by = ?
                WHERE merchant_id = ?
                  AND account_id = ?
                  AND deleted = 0
                """, operatorId, merchantId, accountId);
        for (Long scopeObjectId : scopeObjectIds) {
            jdbcTemplate.update("""
                    INSERT INTO cdd_merchant_account_scope (
                      id, merchant_id, account_id, scope_type, scope_object_type, scope_object_id,
                      action_permissions_json, created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?)
                    """,
                    nextScopeId(accountId, scopeObjectId),
                    merchantId,
                    accountId,
                    scopeType,
                    scopeType,
                    scopeObjectId,
                    "[]",
                    operatorId,
                    operatorId);
        }
    }

    @Override
    public boolean allStoresExist(long merchantId, List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(storeIds.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.addAll(storeIds);
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_merchant_store
                WHERE merchant_id = ?
                  AND deleted = 0
                  AND id IN (""" + placeholders + ")",
                Long.class,
                args.toArray());
        return count != null && count == storeIds.size();
    }

    @Override
    public boolean allMiniProgramsExist(long merchantId, List<Long> miniProgramIds) {
        if (miniProgramIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(miniProgramIds.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.addAll(miniProgramIds);
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_merchant_mini_program
                WHERE merchant_id = ?
                  AND deleted = 0
                  AND id IN (""" + placeholders + ")",
                Long.class,
                args.toArray());
        return count != null && count == miniProgramIds.size();
    }

    @Override
    public long requireRoleId(String roleCode) {
        Long roleId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM cdd_auth_role
                WHERE role_code = ?
                  AND deleted = 0
                  AND status = 'enabled'
                LIMIT 1
                """, Long.class, roleCode);
        return roleId == null ? -1L : roleId;
    }

    @Override
    public void createAuthAccount(StoredAuthAccount authAccount) {
        jdbcTemplate.update("""
                INSERT INTO cdd_auth_account (
                  id, user_id, account_name, display_name, mobile, email, password_hash, account_type,
                  merchant_id, store_id, mini_program_id, status, last_login_at, token_version, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, NULL, ?, 'merchant', ?, ?, ?, ?, NULL, 0, ?, ?)
                """,
                authAccount.accountId(),
                authAccount.userId(),
                authAccount.accountName(),
                authAccount.displayName(),
                authAccount.mobile(),
                authAccount.passwordHash(),
                authAccount.merchantId(),
                authAccount.storeId(),
                authAccount.miniProgramId(),
                authAccount.status(),
                authAccount.operatorId(),
                authAccount.operatorId());
    }

    @Override
    public void updateAuthAccount(StoredAuthAccount authAccount) {
        jdbcTemplate.update("""
                UPDATE cdd_auth_account
                SET account_name = ?,
                    display_name = ?,
                    mobile = ?,
                    merchant_id = ?,
                    store_id = ?,
                    mini_program_id = ?,
                    updated_by = ?
                WHERE id = ?
                  AND deleted = 0
                """,
                authAccount.accountName(),
                authAccount.displayName(),
                authAccount.mobile(),
                authAccount.merchantId(),
                authAccount.storeId(),
                authAccount.miniProgramId(),
                authAccount.operatorId(),
                authAccount.accountId());
    }

    @Override
    public void createAuthAccountRole(long bindingId, long accountId, long roleId, long operatorId) {
        jdbcTemplate.update("""
                INSERT INTO cdd_auth_account_role (
                  id, account_id, role_id, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?)
                """, bindingId, accountId, roleId, operatorId, operatorId);
    }

    @Override
    public void updateAuthPassword(long accountId, String userId, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE cdd_auth_account
                SET password_hash = ?,
                    token_version = token_version + 1,
                    updated_by = 0
                WHERE id = ?
                  AND user_id = ?
                  AND deleted = 0
                """, passwordHash, accountId, userId);
    }

    @Override
    public void disableAuthAccount(long accountId, String userId, long operatorId) {
        jdbcTemplate.update("""
                UPDATE cdd_auth_account
                SET status = 'disabled',
                    token_version = token_version + 1,
                    updated_by = ?
                WHERE id = ?
                  AND user_id = ?
                  AND deleted = 0
                """, operatorId, accountId, userId);
    }

    private static StoredMerchantSubAccount mapSubAccount(ResultSet rs, int rowNum) throws SQLException {
        return new StoredMerchantSubAccount(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getString("account_name"),
                rs.getString("display_name"),
                rs.getString("mobile"),
                rs.getString("remark"),
                rs.getString("status"),
                rs.getString("role_label"),
                List.of(),
                List.of(),
                rs.getString("data_scope_type"),
                List.of());
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    public List<String> loadPermissionModules(long accountId) {
        String raw = jdbcTemplate.queryForObject("""
                SELECT permission_modules_json
                FROM cdd_merchant_sub_account
                WHERE id = ?
                  AND deleted = 0
                """, String.class, accountId);
        return readJsonList(raw);
    }

    public List<String> loadActionPermissions(long accountId) {
        String raw = jdbcTemplate.queryForObject("""
                SELECT action_permissions_json
                FROM cdd_merchant_sub_account
                WHERE id = ?
                  AND deleted = 0
                """, String.class, accountId);
        return readJsonList(raw);
    }

    public List<String> loadDataScopeIds(long accountId, String scopeType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT scope_object_type, scope_object_id
                FROM cdd_merchant_account_scope
                WHERE account_id = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, accountId);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String scopeObjectType = String.valueOf(row.get("scope_object_type"));
            Object objectId = row.get("scope_object_id");
            if (objectId == null) {
                continue;
            }
            result.add(scopeObjectType + "_" + objectId);
        }
        return result;
    }

    private List<String> readJsonList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static long nextScopeId(long accountId, long scopeObjectId) {
        return Math.abs((accountId * 31) + scopeObjectId);
    }
}
