package com.cdd.release.infrastructure.persistence;

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
public class JdbcReleaseGovernanceRepository implements ReleaseGovernanceRepository {

    private static final RowMapper<StoredReleaseTask> RELEASE_TASK_ROW_MAPPER = JdbcReleaseGovernanceRepository::mapReleaseTask;
    private static final RowMapper<StoredReleaseTaskStep> RELEASE_STEP_ROW_MAPPER = JdbcReleaseGovernanceRepository::mapReleaseStep;
    private static final RowMapper<StoredTemplateVersion> TEMPLATE_VERSION_ROW_MAPPER =
            JdbcReleaseGovernanceRepository::mapTemplateVersion;
    private static final RowMapper<StoredReleaseVersionMapping> VERSION_MAPPING_ROW_MAPPER =
            JdbcReleaseGovernanceRepository::mapVersionMapping;

    private final JdbcTemplate jdbcTemplate;

    public JdbcReleaseGovernanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createReleaseTask(StoredReleaseTask task) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_task (
                  id, task_no, merchant_id, store_id, mini_program_id, template_version_id,
                  release_type, release_status, trigger_source, release_snapshot_json,
                  current_step_code, result_sync_status, rollback_task_no, last_error_code,
                  last_error_message, started_at, finished_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                task.id(),
                task.taskNo(),
                task.merchantId(),
                task.storeId(),
                task.miniProgramId(),
                task.templateVersionId(),
                task.releaseType(),
                task.releaseStatus(),
                task.triggerSource(),
                task.releaseSnapshotJson(),
                task.currentStepCode(),
                task.resultSyncStatus(),
                task.rollbackTaskNo(),
                task.lastErrorCode(),
                task.lastErrorMessage(),
                toTimestamp(task.startedAt()),
                toTimestamp(task.finishedAt()),
                0L,
                0L);
    }

    @Override
    public Optional<StoredReleaseTask> findReleaseTaskByTaskNo(String taskNo) {
        List<StoredReleaseTask> rows = jdbcTemplate.query("""
                SELECT id, task_no, merchant_id, store_id, mini_program_id, template_version_id,
                       release_type, release_status, trigger_source, release_snapshot_json,
                       current_step_code, result_sync_status, rollback_task_no, last_error_code,
                       last_error_message, started_at, finished_at
                FROM cdd_release_task
                WHERE task_no = ?
                  AND deleted = 0
                LIMIT 1
                """, RELEASE_TASK_ROW_MAPPER, taskNo);
        return rows.stream().findFirst();
    }

    @Override
    public void updateReleaseTask(StoredReleaseTask task) {
        jdbcTemplate.update("""
                UPDATE cdd_release_task
                SET release_status = ?,
                    trigger_source = ?,
                    release_snapshot_json = ?,
                    current_step_code = ?,
                    result_sync_status = ?,
                    rollback_task_no = ?,
                    last_error_code = ?,
                    last_error_message = ?,
                    started_at = ?,
                    finished_at = ?,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                """,
                task.releaseStatus(),
                task.triggerSource(),
                task.releaseSnapshotJson(),
                task.currentStepCode(),
                task.resultSyncStatus(),
                task.rollbackTaskNo(),
                task.lastErrorCode(),
                task.lastErrorMessage(),
                toTimestamp(task.startedAt()),
                toTimestamp(task.finishedAt()),
                task.id());
    }

    @Override
    public void createReleaseTaskStep(StoredReleaseTaskStep step) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_task_detail (
                  id, task_id, step_code, step_name, step_status, step_order,
                  result_message, error_code, retry_count, started_at, finished_at,
                  created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                step.id(),
                step.taskId(),
                step.stepCode(),
                step.stepName(),
                step.stepStatus(),
                step.stepOrder(),
                step.resultMessage(),
                step.errorCode(),
                step.retryCount(),
                toTimestamp(step.startedAt()),
                toTimestamp(step.finishedAt()),
                0L,
                0L);
    }

    @Override
    public Optional<StoredReleaseTaskStep> findReleaseTaskStep(long taskId, String stepCode) {
        List<StoredReleaseTaskStep> rows = jdbcTemplate.query("""
                SELECT id, task_id, step_code, step_name, step_order, step_status,
                       result_message, error_code, retry_count, started_at, finished_at
                FROM cdd_release_task_detail
                WHERE task_id = ?
                  AND step_code = ?
                  AND deleted = 0
                LIMIT 1
                """, RELEASE_STEP_ROW_MAPPER, taskId, stepCode);
        return rows.stream().findFirst();
    }

    @Override
    public void updateReleaseTaskStep(StoredReleaseTaskStep step) {
        jdbcTemplate.update("""
                UPDATE cdd_release_task_detail
                SET step_name = ?,
                    step_order = ?,
                    step_status = ?,
                    result_message = ?,
                    error_code = ?,
                    retry_count = ?,
                    started_at = ?,
                    finished_at = ?,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                """,
                step.stepName(),
                step.stepOrder(),
                step.stepStatus(),
                step.resultMessage(),
                step.errorCode(),
                step.retryCount(),
                toTimestamp(step.startedAt()),
                toTimestamp(step.finishedAt()),
                step.id());
    }

    @Override
    public List<StoredReleaseTaskStep> listReleaseTaskSteps(long taskId) {
        return jdbcTemplate.query("""
                SELECT id, task_id, step_code, step_name, step_order, step_status,
                       result_message, error_code, retry_count, started_at, finished_at
                FROM cdd_release_task_detail
                WHERE task_id = ?
                  AND deleted = 0
                ORDER BY step_order ASC, id ASC
                """, RELEASE_STEP_ROW_MAPPER, taskId);
    }

    @Override
    public void appendReleaseLog(long logId,
                                 long taskId,
                                 String logLevel,
                                 String logStage,
                                 String logContent,
                                 Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_log (
                  id, task_id, log_level, log_stage, log_content, created_by, updated_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, logId, taskId, logLevel, logStage, logContent, 0L, 0L, toTimestamp(createdAt));
    }

    @Override
    public Optional<StoredTemplateVersion> findTemplateVersionById(long templateVersionId) {
        List<StoredTemplateVersion> rows = jdbcTemplate.query("""
                SELECT id, template_code, template_version
                FROM cdd_template_version
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, TEMPLATE_VERSION_ROW_MAPPER, templateVersionId);
        return rows.stream().findFirst();
    }

    @Override
    public boolean miniProgramExists(long merchantId, long storeId, long miniProgramId) {
        List<Long> rows = jdbcTemplate.queryForList("""
                SELECT id
                FROM cdd_merchant_mini_program
                WHERE id = ?
                  AND merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                LIMIT 1
                """, Long.class, miniProgramId, merchantId, storeId);
        return !rows.isEmpty();
    }

    @Override
    public void updateMiniProgramTemplateVersion(long miniProgramId, String currentTemplateVersion) {
        jdbcTemplate.update("""
                UPDATE cdd_merchant_mini_program
                SET current_template_version = ?,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                """, currentTemplateVersion, miniProgramId);
    }

    @Override
    public void deactivateVersionMappings(long miniProgramId, Instant deactivatedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_release_version_mapping
                SET mapping_status = 'inactive',
                    deactivated_at = ?,
                    updated_by = 0
                WHERE mini_program_id = ?
                  AND mapping_status = 'active'
                  AND deleted = 0
                """, toTimestamp(deactivatedAt), miniProgramId);
    }

    @Override
    public Optional<StoredReleaseVersionMapping> findVersionMapping(long miniProgramId, long templateVersionId) {
        List<StoredReleaseVersionMapping> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, mini_program_id, template_version_id,
                       template_code, template_version, mapping_status, source_task_id,
                       activated_at, deactivated_at
                FROM cdd_release_version_mapping
                WHERE mini_program_id = ?
                  AND template_version_id = ?
                  AND deleted = 0
                LIMIT 1
                """, VERSION_MAPPING_ROW_MAPPER, miniProgramId, templateVersionId);
        return rows.stream().findFirst();
    }

    @Override
    public void createVersionMapping(StoredReleaseVersionMapping mapping) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_version_mapping (
                  id, merchant_id, store_id, mini_program_id, template_version_id,
                  template_code, template_version, mapping_status, source_task_id,
                  activated_at, deactivated_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                mapping.id(),
                mapping.merchantId(),
                mapping.storeId(),
                mapping.miniProgramId(),
                mapping.templateVersionId(),
                mapping.templateCode(),
                mapping.templateVersion(),
                mapping.mappingStatus(),
                mapping.sourceTaskId(),
                toTimestamp(mapping.activatedAt()),
                toTimestamp(mapping.deactivatedAt()),
                0L,
                0L);
    }

    @Override
    public void updateVersionMapping(StoredReleaseVersionMapping mapping) {
        jdbcTemplate.update("""
                UPDATE cdd_release_version_mapping
                SET mapping_status = ?,
                    source_task_id = ?,
                    activated_at = ?,
                    deactivated_at = ?,
                    updated_by = 0
                WHERE id = ?
                  AND deleted = 0
                """,
                mapping.mappingStatus(),
                mapping.sourceTaskId(),
                toTimestamp(mapping.activatedAt()),
                toTimestamp(mapping.deactivatedAt()),
                mapping.id());
    }

    @Override
    public void createRollbackRecord(StoredRollbackRecord rollbackRecord) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_rollback_record (
                  id, task_id, rollback_target_version, rollback_reason, rollback_status,
                  rolled_back_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                rollbackRecord.id(),
                rollbackRecord.taskId(),
                rollbackRecord.rollbackTargetVersion(),
                rollbackRecord.rollbackReason(),
                rollbackRecord.rollbackStatus(),
                toTimestamp(rollbackRecord.rolledBackAt()),
                0L,
                0L);
    }

    private static StoredReleaseTask mapReleaseTask(ResultSet rs, int rowNum) throws SQLException {
        return new StoredReleaseTask(
                rs.getLong("id"),
                rs.getString("task_no"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("mini_program_id"),
                rs.getLong("template_version_id"),
                rs.getString("release_type"),
                rs.getString("release_status"),
                rs.getString("trigger_source"),
                rs.getString("release_snapshot_json"),
                rs.getString("current_step_code"),
                rs.getString("result_sync_status"),
                rs.getString("rollback_task_no"),
                rs.getString("last_error_code"),
                rs.getString("last_error_message"),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("finished_at")));
    }

    private static StoredReleaseTaskStep mapReleaseStep(ResultSet rs, int rowNum) throws SQLException {
        return new StoredReleaseTaskStep(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getString("step_code"),
                rs.getString("step_name"),
                rs.getInt("step_order"),
                rs.getString("step_status"),
                rs.getString("result_message"),
                rs.getString("error_code"),
                rs.getInt("retry_count"),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("finished_at")));
    }

    private static StoredTemplateVersion mapTemplateVersion(ResultSet rs, int rowNum) throws SQLException {
        return new StoredTemplateVersion(
                rs.getLong("id"),
                rs.getString("template_code"),
                rs.getString("template_version"));
    }

    private static StoredReleaseVersionMapping mapVersionMapping(ResultSet rs, int rowNum) throws SQLException {
        return new StoredReleaseVersionMapping(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("mini_program_id"),
                rs.getLong("template_version_id"),
                rs.getString("template_code"),
                rs.getString("template_version"),
                rs.getString("mapping_status"),
                rs.getLong("source_task_id"),
                toInstant(rs.getTimestamp("activated_at")),
                toInstant(rs.getTimestamp("deactivated_at")));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
