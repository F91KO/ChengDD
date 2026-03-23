package com.cdd.decoration.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDecorationRepository implements DecorationRepository {

    private static final RowMapper<HomeConfigRecord> HOME_CONFIG_ROW_MAPPER = JdbcDecorationRepository::mapHomeConfig;
    private static final RowMapper<HomeModuleRecord> HOME_MODULE_ROW_MAPPER = JdbcDecorationRepository::mapHomeModule;
    private static final RowMapper<HomeVersionRecord> HOME_VERSION_ROW_MAPPER = JdbcDecorationRepository::mapHomeVersion;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDecorationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<HomeConfigRecord> findHomeConfig(long merchantId, long storeId, long miniProgramId) {
        List<HomeConfigRecord> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, mini_program_id, home_template_code, home_style_mode, theme_color,
                       search_placeholder, header_service_bar_json, brand_hero_block_json, delivery_promise_text,
                       delivery_fee_text, minimum_order_text, announcement_text, status, version_no
                FROM cdd_decoration_home_config
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND mini_program_id = ?
                  AND deleted = 0
                LIMIT 1
                """, HOME_CONFIG_ROW_MAPPER, merchantId, storeId, miniProgramId);
        return rows.stream().findFirst();
    }

    @Override
    public List<HomeModuleRecord> listHomeModules(long homeConfigId) {
        return jdbcTemplate.query("""
                SELECT id, home_config_id, module_type, module_name, sort_order, is_enabled, style_mode,
                       data_source_type, data_source_id, jump_target_json, config_payload_json
                FROM cdd_decoration_home_module
                WHERE home_config_id = ?
                  AND deleted = 0
                ORDER BY sort_order ASC, id ASC
                """, HOME_MODULE_ROW_MAPPER, homeConfigId);
    }

    @Override
    public HomeConfigRecord saveHomeConfig(HomeConfigUpsertCommand command) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_decoration_home_config
                SET home_template_code = ?, home_style_mode = ?, theme_color = ?, search_placeholder = ?,
                    header_service_bar_json = ?, brand_hero_block_json = ?, delivery_promise_text = ?,
                    delivery_fee_text = ?, minimum_order_text = ?, announcement_text = ?, status = ?, version_no = ?,
                    updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND mini_program_id = ?
                  AND deleted = 0
                """,
                command.homeTemplateCode(),
                command.homeStyleMode(),
                command.themeColor(),
                command.searchPlaceholder(),
                command.headerServiceBarJson(),
                command.brandHeroBlockJson(),
                command.deliveryPromiseText(),
                command.deliveryFeeText(),
                command.minimumOrderText(),
                command.announcementText(),
                command.status(),
                command.versionNo(),
                command.merchantId(),
                command.merchantId(),
                command.storeId(),
                command.miniProgramId());
        if (updated == 0) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO cdd_decoration_home_config
                        (id, merchant_id, store_id, mini_program_id, home_template_code, home_style_mode, theme_color,
                         search_placeholder, header_service_bar_json, brand_hero_block_json, delivery_promise_text,
                         delivery_fee_text, minimum_order_text, announcement_text, status, version_no,
                         created_by, updated_by, deleted, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                        """,
                        command.id(),
                        command.merchantId(),
                        command.storeId(),
                        command.miniProgramId(),
                        command.homeTemplateCode(),
                        command.homeStyleMode(),
                        command.themeColor(),
                        command.searchPlaceholder(),
                        command.headerServiceBarJson(),
                        command.brandHeroBlockJson(),
                        command.deliveryPromiseText(),
                        command.deliveryFeeText(),
                        command.minimumOrderText(),
                        command.announcementText(),
                        command.status(),
                        command.versionNo(),
                        command.merchantId(),
                        command.merchantId());
            } catch (DuplicateKeyException ex) {
                jdbcTemplate.update("""
                        UPDATE cdd_decoration_home_config
                        SET home_template_code = ?, home_style_mode = ?, theme_color = ?, search_placeholder = ?,
                            header_service_bar_json = ?, brand_hero_block_json = ?, delivery_promise_text = ?,
                            delivery_fee_text = ?, minimum_order_text = ?, announcement_text = ?, status = ?, version_no = ?,
                            updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE merchant_id = ?
                          AND store_id = ?
                          AND mini_program_id = ?
                          AND deleted = 0
                        """,
                        command.homeTemplateCode(),
                        command.homeStyleMode(),
                        command.themeColor(),
                        command.searchPlaceholder(),
                        command.headerServiceBarJson(),
                        command.brandHeroBlockJson(),
                        command.deliveryPromiseText(),
                        command.deliveryFeeText(),
                        command.minimumOrderText(),
                        command.announcementText(),
                        command.status(),
                        command.versionNo(),
                        command.merchantId(),
                        command.merchantId(),
                        command.storeId(),
                        command.miniProgramId());
            }
        }
        return findHomeConfig(command.merchantId(), command.storeId(), command.miniProgramId()).orElseThrow();
    }

    @Override
    public void replaceHomeModules(long merchantId, long storeId, long homeConfigId, List<HomeModuleDraft> modules) {
        jdbcTemplate.update("""
                UPDATE cdd_decoration_home_module
                SET deleted = 1, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE home_config_id = ?
                  AND deleted = 0
                """, merchantId, homeConfigId);
        for (HomeModuleDraft module : modules) {
            jdbcTemplate.update("""
                    INSERT INTO cdd_decoration_home_module
                    (id, merchant_id, store_id, home_config_id, module_type, module_name, sort_order, is_enabled,
                     style_mode, data_source_type, data_source_id, jump_target_json, config_payload_json,
                     created_by, updated_by, deleted, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                    """,
                    module.id(),
                    merchantId,
                    storeId,
                    homeConfigId,
                    module.moduleType(),
                    module.moduleName(),
                    module.sortOrder(),
                    module.enabled() ? 1 : 0,
                    module.styleMode(),
                    module.dataSourceType(),
                    module.dataSourceId(),
                    module.jumpTargetJson(),
                    module.configPayloadJson(),
                    merchantId,
                    merchantId);
        }
    }

    @Override
    public void upsertTemplateBinding(long id,
                                      long merchantId,
                                      long storeId,
                                      long miniProgramId,
                                      String templateCode,
                                      String styleMode,
                                      String bindingStatus) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_decoration_template_binding
                SET style_mode = ?, binding_status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND mini_program_id = ?
                  AND template_code = ?
                  AND deleted = 0
                """, styleMode, bindingStatus, merchantId, merchantId, storeId, miniProgramId, templateCode);
        if (updated > 0) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_decoration_template_binding
                    (id, merchant_id, store_id, mini_program_id, template_code, style_mode, binding_status,
                     created_by, updated_by, deleted, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                    """,
                    id, merchantId, storeId, miniProgramId, templateCode, styleMode, bindingStatus, merchantId, merchantId);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update("""
                    UPDATE cdd_decoration_template_binding
                    SET style_mode = ?, binding_status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE merchant_id = ?
                      AND store_id = ?
                      AND mini_program_id = ?
                      AND template_code = ?
                      AND deleted = 0
                    """, styleMode, bindingStatus, merchantId, merchantId, storeId, miniProgramId, templateCode);
        }
    }

    @Override
    public void createHomeVersion(long id,
                                  long merchantId,
                                  long storeId,
                                  long homeConfigId,
                                  int versionNo,
                                  String versionStatus,
                                  String snapshotJson,
                                  Instant publishedAt) {
        jdbcTemplate.update("""
                INSERT INTO cdd_decoration_home_version
                (id, merchant_id, store_id, home_config_id, version_no, version_status, snapshot_json, published_at,
                 created_by, updated_by, deleted, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id,
                merchantId,
                storeId,
                homeConfigId,
                versionNo,
                versionStatus,
                snapshotJson,
                publishedAt == null ? null : Timestamp.from(publishedAt),
                merchantId,
                merchantId);
    }

    @Override
    public List<HomeVersionRecord> listHomeVersions(long homeConfigId) {
        return jdbcTemplate.query("""
                SELECT id, home_config_id, version_no, version_status, snapshot_json
                FROM cdd_decoration_home_version
                WHERE home_config_id = ?
                  AND deleted = 0
                ORDER BY version_no DESC, id DESC
                """, HOME_VERSION_ROW_MAPPER, homeConfigId);
    }

    private static HomeConfigRecord mapHomeConfig(ResultSet rs, int rowNum) throws SQLException {
        return new HomeConfigRecord(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("mini_program_id"),
                rs.getString("home_template_code"),
                rs.getString("home_style_mode"),
                rs.getString("theme_color"),
                rs.getString("search_placeholder"),
                rs.getString("header_service_bar_json"),
                rs.getString("brand_hero_block_json"),
                rs.getString("delivery_promise_text"),
                rs.getString("delivery_fee_text"),
                rs.getString("minimum_order_text"),
                rs.getString("announcement_text"),
                rs.getString("status"),
                rs.getInt("version_no"));
    }

    private static HomeModuleRecord mapHomeModule(ResultSet rs, int rowNum) throws SQLException {
        return new HomeModuleRecord(
                rs.getLong("id"),
                rs.getLong("home_config_id"),
                rs.getString("module_type"),
                rs.getString("module_name"),
                rs.getInt("sort_order"),
                rs.getBoolean("is_enabled"),
                rs.getString("style_mode"),
                rs.getString("data_source_type"),
                rs.getObject("data_source_id", Long.class),
                rs.getString("jump_target_json"),
                rs.getString("config_payload_json"));
    }

    private static HomeVersionRecord mapHomeVersion(ResultSet rs, int rowNum) throws SQLException {
        return new HomeVersionRecord(
                rs.getLong("id"),
                rs.getLong("home_config_id"),
                rs.getInt("version_no"),
                rs.getString("version_status"),
                rs.getString("snapshot_json"));
    }
}
