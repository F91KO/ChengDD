package com.cdd.merchant.infrastructure.persistence;

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
public class JdbcMerchantOnboardingRepository implements MerchantOnboardingRepository {

    private static final RowMapper<StoredMerchantApplication> APPLICATION_ROW_MAPPER =
            JdbcMerchantOnboardingRepository::mapApplication;

    private final JdbcTemplate jdbcTemplate;

    public JdbcMerchantOnboardingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createApplication(StoredMerchantApplication application) {
        jdbcTemplate.update("""
                INSERT INTO cdd_merchant_application (
                  id, merchant_name, merchant_type, contact_name, contact_mobile,
                  legal_person_name, business_category, brand_name, license_file_url,
                  status, reject_reason, submitted_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                application.applicationId(),
                application.merchantName(),
                application.merchantType(),
                application.contactName(),
                application.contactMobile(),
                application.legalPersonName(),
                application.businessCategory(),
                application.brandName(),
                application.licenseFileUrl(),
                application.status(),
                application.rejectReason(),
                toTimestamp(application.submittedAt()),
                0L,
                0L);
    }

    @Override
    public Optional<StoredMerchantApplication> findApplicationById(long applicationId) {
        List<StoredMerchantApplication> rows = jdbcTemplate.query("""
                SELECT id, merchant_name, merchant_type, contact_name, contact_mobile,
                       legal_person_name, business_category, brand_name, license_file_url,
                       status, reject_reason, submitted_at
                FROM cdd_merchant_application
                WHERE id = ?
                  AND deleted = 0
                """, APPLICATION_ROW_MAPPER, applicationId);
        return rows.stream().findFirst();
    }

    @Override
    public boolean submitApplication(long applicationId, Instant submittedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_merchant_application
                SET status = 'submitted',
                    submitted_at = ?,
                    reject_reason = NULL,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                  AND status IN ('draft', 'supplement_required', 'rejected')
                """, Timestamp.from(submittedAt), applicationId);
        return updated > 0;
    }

    @Override
    public boolean approveApplication(long applicationId) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_merchant_application
                SET status = 'approved',
                    reject_reason = NULL,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                  AND status IN ('submitted', 'reviewing')
                """, applicationId);
        return updated > 0;
    }

    @Override
    public boolean rejectApplication(long applicationId, String rejectReason) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_merchant_application
                SET status = 'rejected',
                    reject_reason = ?,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                  AND status IN ('submitted', 'reviewing')
                """, rejectReason, applicationId);
        return updated > 0;
    }

    @Override
    public Optional<Long> findMerchantProfileIdByApplicationId(long applicationId) {
        List<Long> rows = jdbcTemplate.queryForList("""
                SELECT id
                FROM cdd_merchant_profile
                WHERE current_application_id = ?
                  AND deleted = 0
                LIMIT 1
                """, Long.class, applicationId);
        return rows.stream().findFirst();
    }

    @Override
    public void createMerchantProfile(long profileId,
                                      String merchantNo,
                                      StoredMerchantApplication application,
                                      Instant settledAt) {
        jdbcTemplate.update("""
                INSERT INTO cdd_merchant_profile (
                  id, merchant_no, merchant_name, merchant_type, brand_name,
                  status, current_application_id, contact_name, contact_mobile,
                  business_scope, settled_at, remark, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, 'pending_activation', ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                profileId,
                merchantNo,
                application.merchantName(),
                application.merchantType(),
                application.brandName(),
                application.applicationId(),
                application.contactName(),
                application.contactMobile(),
                application.businessCategory(),
                Timestamp.from(settledAt),
                "一键开通初始化",
                0L,
                0L);
    }

    @Override
    public void createStore(long storeId,
                            long merchantId,
                            String storeNo,
                            String storeName) {
        jdbcTemplate.update("""
                INSERT INTO cdd_merchant_store (
                  id, merchant_id, store_no, store_name, business_status, created_by, updated_by
                ) VALUES (?, ?, ?, ?, 'preparing', ?, ?)
                """,
                storeId,
                merchantId,
                storeNo,
                storeName,
                0L,
                0L);
    }

    @Override
    public void createMiniProgram(long miniProgramId,
                                  long merchantId,
                                  long storeId,
                                  String appId,
                                  String appSecretMasked,
                                  String paymentMchId,
                                  String serverDomain,
                                  String detectResultJson) {
        jdbcTemplate.update("""
                INSERT INTO cdd_merchant_mini_program (
                  id, merchant_id, store_id, app_id, app_secret_masked, payment_mch_id,
                  server_domain, binding_status, current_template_version, last_detect_result_json,
                  created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'detecting', NULL, CAST(? AS JSON), ?, ?)
                """,
                miniProgramId,
                merchantId,
                storeId,
                appId,
                appSecretMasked,
                paymentMchId,
                serverDomain,
                detectResultJson,
                0L,
                0L);
    }

    @Override
    public void createOnboardingTask(StoredOnboardingTask task) {
        jdbcTemplate.update("""
                INSERT INTO cdd_merchant_onboarding_task (
                  id, task_no, application_id, merchant_id, store_id, mini_program_id,
                  task_status, step_code, validation_result_json, error_message,
                  started_at, finished_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                task.id(),
                task.taskNo(),
                task.applicationId(),
                nullable(task.merchantId()),
                nullable(task.storeId()),
                nullable(task.miniProgramId()),
                task.taskStatus(),
                task.stepCode(),
                task.validationResultJson(),
                task.errorMessage(),
                toTimestamp(task.startedAt()),
                toTimestamp(task.finishedAt()),
                0L,
                0L);
    }

    @Override
    public void markOnboardingTaskFailed(String taskNo,
                                         String stepCode,
                                         String errorMessage,
                                         Instant finishedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_onboarding_task
                SET task_status = 'failed',
                    step_code = ?,
                    error_message = ?,
                    finished_at = ?,
                    updated_by = 0
                WHERE task_no = ?
                  AND deleted = 0
                """, stepCode, errorMessage, Timestamp.from(finishedAt), taskNo);
    }

    @Override
    public void markOnboardingTaskSuccess(String taskNo,
                                          String stepCode,
                                          long merchantId,
                                          long storeId,
                                          long miniProgramId,
                                          Instant finishedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_onboarding_task
                SET task_status = 'success',
                    step_code = ?,
                    merchant_id = ?,
                    store_id = ?,
                    mini_program_id = ?,
                    error_message = NULL,
                    finished_at = ?,
                    updated_by = 0
                WHERE task_no = ?
                  AND deleted = 0
                """,
                stepCode,
                merchantId,
                storeId,
                miniProgramId,
                Timestamp.from(finishedAt),
                taskNo);
    }

    @Override
    public void activateProfileAndStore(long merchantId,
                                        long storeId) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_profile
                SET status = 'active',
                    settled_at = COALESCE(settled_at, CURRENT_TIMESTAMP),
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                """, merchantId);
        jdbcTemplate.update("""
                UPDATE cdd_merchant_store
                SET business_status = 'open',
                    updated_by = 0
                WHERE id = ?
                  AND merchant_id = ?
                  AND deleted = 0
                """, storeId, merchantId);
    }

    private static StoredMerchantApplication mapApplication(ResultSet rs, int rowNum) throws SQLException {
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        return new StoredMerchantApplication(
                rs.getLong("id"),
                rs.getString("merchant_name"),
                rs.getString("merchant_type"),
                rs.getString("contact_name"),
                rs.getString("contact_mobile"),
                rs.getString("legal_person_name"),
                rs.getString("business_category"),
                rs.getString("brand_name"),
                rs.getString("license_file_url"),
                rs.getString("status"),
                rs.getString("reject_reason"),
                submittedAt == null ? null : submittedAt.toInstant());
    }

    private static Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Long nullable(long value) {
        return value <= 0 ? null : value;
    }
}
