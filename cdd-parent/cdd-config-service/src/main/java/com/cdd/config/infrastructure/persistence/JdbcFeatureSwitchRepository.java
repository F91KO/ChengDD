package com.cdd.config.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFeatureSwitchRepository implements FeatureSwitchRepository {

    private static final RowMapper<FeatureSwitchDefinitionRecord> DEFINITION_ROW_MAPPER =
            JdbcFeatureSwitchRepository::mapDefinitionRow;
    private static final RowMapper<FeatureSwitchMerchantOverrideRecord> OVERRIDE_ROW_MAPPER =
            JdbcFeatureSwitchRepository::mapOverrideRow;

    private final JdbcTemplate jdbcTemplate;

    public JdbcFeatureSwitchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertDefinition(long id,
                                 String switchCode,
                                 String switchName,
                                 String switchScope,
                                 String defaultValue,
                                 String status) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_config_feature_switch
                SET switch_name = ?,
                    switch_scope = ?,
                    default_value = ?,
                    status = ?,
                    updated_by = 0,
                    updated_at = CURRENT_TIMESTAMP
                WHERE switch_code = ?
                  AND deleted = 0
                """, switchName, switchScope, defaultValue, status, switchCode);
        if (updated > 0) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_config_feature_switch
                    (id, switch_code, switch_name, switch_scope, default_value, status, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?, 0, 0)
                    """, id, switchCode, switchName, switchScope, defaultValue, status);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update("""
                    UPDATE cdd_config_feature_switch
                    SET switch_name = ?,
                        switch_scope = ?,
                        default_value = ?,
                        status = ?,
                        updated_by = 0,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE switch_code = ?
                      AND deleted = 0
                    """, switchName, switchScope, defaultValue, status, switchCode);
        }
    }

    @Override
    public boolean updateStatus(String switchCode, String status) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_config_feature_switch
                SET status = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE switch_code = ?
                  AND deleted = 0
                """, status, switchCode);
        return updated > 0;
    }

    @Override
    public Optional<FeatureSwitchDefinitionRecord> findByCode(String switchCode) {
        List<FeatureSwitchDefinitionRecord> rows = jdbcTemplate.query("""
                SELECT id, switch_code, switch_name, switch_scope, default_value, status
                FROM cdd_config_feature_switch
                WHERE switch_code = ?
                  AND deleted = 0
                """, DEFINITION_ROW_MAPPER, switchCode);
        return rows.stream().findFirst();
    }

    @Override
    public List<FeatureSwitchDefinitionRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT id, switch_code, switch_name, switch_scope, default_value, status
                FROM cdd_config_feature_switch
                WHERE deleted = 0
                ORDER BY switch_code ASC
                """, DEFINITION_ROW_MAPPER);
    }

    @Override
    public void upsertMerchantOverride(long id, long switchId, String merchantId, String switchValue) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_config_feature_switch_merchant_override
                SET switch_value = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE switch_id = ?
                  AND merchant_id = ?
                  AND deleted = 0
                """, switchValue, switchId, merchantId);
        if (updated > 0) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_config_feature_switch_merchant_override
                    (id, switch_id, merchant_id, switch_value, created_by, updated_by)
                    VALUES (?, ?, ?, ?, 0, 0)
                    """, id, switchId, merchantId, switchValue);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update("""
                    UPDATE cdd_config_feature_switch_merchant_override
                    SET switch_value = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                    WHERE switch_id = ?
                      AND merchant_id = ?
                      AND deleted = 0
                    """, switchValue, switchId, merchantId);
        }
    }

    @Override
    public Optional<FeatureSwitchMerchantOverrideRecord> findMerchantOverride(long switchId, String merchantId) {
        List<FeatureSwitchMerchantOverrideRecord> rows = jdbcTemplate.query("""
                SELECT switch_id, merchant_id, switch_value
                FROM cdd_config_feature_switch_merchant_override
                WHERE switch_id = ?
                  AND merchant_id = ?
                  AND deleted = 0
                """, OVERRIDE_ROW_MAPPER, switchId, merchantId);
        return rows.stream().findFirst();
    }

    @Override
    public void softDeleteDefinitionsNotIn(List<String> switchCodes) {
        if (switchCodes.isEmpty()) {
            jdbcTemplate.update("""
                    UPDATE cdd_config_feature_switch
                    SET deleted = 1, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                    WHERE deleted = 0
                    """);
            return;
        }
        StringBuilder sql = new StringBuilder("""
                UPDATE cdd_config_feature_switch
                SET deleted = 1, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE deleted = 0
                  AND switch_code NOT IN (
                """);
        Object[] args = new Object[switchCodes.size()];
        for (int index = 0; index < switchCodes.size(); index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("?");
            args[index] = switchCodes.get(index);
        }
        sql.append(")");
        jdbcTemplate.update(sql.toString(), args);
    }

    @Override
    public void softDeleteMerchantOverrides(String merchantId) {
        jdbcTemplate.update("""
                UPDATE cdd_config_feature_switch_merchant_override
                SET deleted = 1, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE merchant_id = ?
                  AND deleted = 0
                """, merchantId);
    }

    private static FeatureSwitchDefinitionRecord mapDefinitionRow(ResultSet rs, int rowNum) throws SQLException {
        return new FeatureSwitchDefinitionRecord(
                rs.getLong("id"),
                rs.getString("switch_code"),
                rs.getString("switch_name"),
                rs.getString("switch_scope"),
                rs.getString("default_value"),
                rs.getString("status"));
    }

    private static FeatureSwitchMerchantOverrideRecord mapOverrideRow(ResultSet rs, int rowNum) throws SQLException {
        return new FeatureSwitchMerchantOverrideRecord(
                rs.getLong("switch_id"),
                rs.getString("merchant_id"),
                rs.getString("switch_value"));
    }
}
