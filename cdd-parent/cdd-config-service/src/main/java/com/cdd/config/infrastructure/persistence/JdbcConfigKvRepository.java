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
public class JdbcConfigKvRepository implements ConfigKvRepository {

    private static final RowMapper<ConfigKvRecord> PLATFORM_ROW_MAPPER = JdbcConfigKvRepository::mapPlatformRow;
    private static final RowMapper<ConfigKvRecord> MERCHANT_ROW_MAPPER = JdbcConfigKvRepository::mapMerchantRow;

    private final JdbcTemplate jdbcTemplate;

    public JdbcConfigKvRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertPlatform(long id, String configGroup, String configKey, String configValue, String configDesc) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_config_kv
                SET config_value = ?, config_desc = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE config_group = ?
                  AND config_key = ?
                  AND deleted = 0
                """, configValue, configDesc, configGroup, configKey);
        if (updated > 0) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_config_kv
                    (id, config_group, config_key, config_value, config_desc, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, 0, 0)
                    """, id, configGroup, configKey, configValue, configDesc);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update("""
                    UPDATE cdd_config_kv
                    SET config_value = ?, config_desc = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                    WHERE config_group = ?
                      AND config_key = ?
                      AND deleted = 0
                    """, configValue, configDesc, configGroup, configKey);
        }
    }

    @Override
    public void upsertMerchantOverride(long id, String merchantId, String configGroup, String configKey, String configValue) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_config_kv_merchant_override
                SET config_value = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE merchant_id = ?
                  AND config_group = ?
                  AND config_key = ?
                  AND deleted = 0
                """, configValue, merchantId, configGroup, configKey);
        if (updated > 0) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_config_kv_merchant_override
                    (id, merchant_id, config_group, config_key, config_value, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, 0, 0)
                    """, id, merchantId, configGroup, configKey, configValue);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update("""
                    UPDATE cdd_config_kv_merchant_override
                    SET config_value = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                    WHERE merchant_id = ?
                      AND config_group = ?
                      AND config_key = ?
                      AND deleted = 0
                    """, configValue, merchantId, configGroup, configKey);
        }
    }

    @Override
    public Optional<ConfigKvRecord> findPlatform(String configGroup, String configKey) {
        List<ConfigKvRecord> rows = jdbcTemplate.query("""
                SELECT config_group, config_key, config_value, config_desc
                FROM cdd_config_kv
                WHERE config_group = ?
                  AND config_key = ?
                  AND deleted = 0
                """, PLATFORM_ROW_MAPPER, configGroup, configKey);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<ConfigKvRecord> findMerchantOverride(String merchantId, String configGroup, String configKey) {
        List<ConfigKvRecord> rows = jdbcTemplate.query("""
                SELECT merchant_id, config_group, config_key, config_value
                FROM cdd_config_kv_merchant_override
                WHERE merchant_id = ?
                  AND config_group = ?
                  AND config_key = ?
                  AND deleted = 0
                """, MERCHANT_ROW_MAPPER, merchantId, configGroup, configKey);
        return rows.stream().findFirst();
    }

    private static ConfigKvRecord mapPlatformRow(ResultSet rs, int rowNum) throws SQLException {
        return new ConfigKvRecord(
                rs.getString("config_group"),
                rs.getString("config_key"),
                rs.getString("config_value"),
                rs.getString("config_desc"),
                null);
    }

    private static ConfigKvRecord mapMerchantRow(ResultSet rs, int rowNum) throws SQLException {
        return new ConfigKvRecord(
                rs.getString("config_group"),
                rs.getString("config_key"),
                rs.getString("config_value"),
                null,
                rs.getString("merchant_id"));
    }
}
