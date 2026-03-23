package com.cdd.config.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcConfigPublishRepository implements ConfigPublishRepository {

    private static final RowMapper<PublishTaskRecord> TASK_ROW_MAPPER = JdbcConfigPublishRepository::mapTask;
    private static final RowMapper<PublishStepRecord> STEP_ROW_MAPPER = JdbcConfigPublishRepository::mapStep;
    private static final RowMapper<PublishRollbackRecord> ROLLBACK_ROW_MAPPER = JdbcConfigPublishRepository::mapRollback;

    private final JdbcTemplate jdbcTemplate;

    public JdbcConfigPublishRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createTask(PublishTaskRecord task) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_task (
                  id, task_no, merchant_id, store_id, mini_program_id, template_version_id,
                  release_type, release_status, trigger_source, release_snapshot_json,
                  current_step_code, result_sync_status, rollback_task_no, last_error_code,
                  last_error_message, started_at, finished_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                task.id(),
                task.taskNo(),
                task.merchantId(),
                task.storeId(),
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
                toTimestamp(task.finishedAt()));
    }

    @Override
    public Optional<PublishTaskRecord> findTaskByTaskNo(String taskNo) {
        return jdbcTemplate.query("""
                SELECT id, task_no, merchant_id, store_id, release_type, release_status, trigger_source,
                       release_snapshot_json, current_step_code, result_sync_status, rollback_task_no,
                       last_error_code, last_error_message, started_at, finished_at, created_at
                FROM cdd_release_task
                WHERE task_no = ?
                  AND deleted = 0
                  AND release_type IN ('config_publish', 'config_rollback')
                LIMIT 1
                """, TASK_ROW_MAPPER, taskNo).stream().findFirst();
    }

    @Override
    public List<PublishTaskRecord> listTasks(Long merchantId, Long storeId) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, task_no, merchant_id, store_id, release_type, release_status, trigger_source,
                       release_snapshot_json, current_step_code, result_sync_status, rollback_task_no,
                       last_error_code, last_error_message, started_at, finished_at, created_at
                FROM cdd_release_task
                WHERE deleted = 0
                  AND release_type IN ('config_publish', 'config_rollback')
                """);
        List<Object> args = new ArrayList<>();
        if (merchantId != null) {
            sql.append(" AND merchant_id = ?");
            args.add(merchantId);
        }
        if (storeId != null) {
            sql.append(" AND store_id = ?");
            args.add(storeId);
        }
        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), TASK_ROW_MAPPER, args.toArray());
    }

    @Override
    public void updateRollbackTaskNo(long taskId, String rollbackTaskNo) {
        jdbcTemplate.update("""
                UPDATE cdd_release_task
                SET rollback_task_no = ?, updated_by = 0, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """, rollbackTaskNo, taskId);
    }

    @Override
    public void createStep(PublishStepRecord step) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_task_detail (
                  id, task_id, step_code, step_name, step_status, step_order,
                  result_message, error_code, retry_count, started_at, finished_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
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
                toTimestamp(step.finishedAt()));
    }

    @Override
    public List<PublishStepRecord> listSteps(long taskId) {
        return jdbcTemplate.query("""
                SELECT id, task_id, step_code, step_name, step_order, step_status,
                       result_message, error_code, retry_count, started_at, finished_at
                FROM cdd_release_task_detail
                WHERE task_id = ?
                  AND deleted = 0
                ORDER BY step_order ASC, id ASC
                """, STEP_ROW_MAPPER, taskId);
    }

    @Override
    public void createRollbackRecord(PublishRollbackRecord rollbackRecord) {
        jdbcTemplate.update("""
                INSERT INTO cdd_release_rollback_record (
                  id, task_id, rollback_target_version, rollback_reason, rollback_status,
                  rolled_back_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, 0, 0)
                """,
                rollbackRecord.id(),
                rollbackRecord.taskId(),
                rollbackRecord.rollbackTargetTaskNo(),
                rollbackRecord.rollbackReason(),
                rollbackRecord.rollbackStatus(),
                toTimestamp(rollbackRecord.rolledBackAt()));
    }

    @Override
    public Optional<PublishRollbackRecord> findRollbackRecord(long taskId) {
        return jdbcTemplate.query("""
                SELECT id, task_id, rollback_target_version, rollback_reason, rollback_status, rolled_back_at
                FROM cdd_release_rollback_record
                WHERE task_id = ?
                  AND deleted = 0
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, ROLLBACK_ROW_MAPPER, taskId).stream().findFirst();
    }

    private static PublishTaskRecord mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new PublishTaskRecord(
                rs.getLong("id"),
                rs.getString("task_no"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
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
                toInstant(rs.getTimestamp("finished_at")),
                toInstant(rs.getTimestamp("created_at")));
    }

    private static PublishStepRecord mapStep(ResultSet rs, int rowNum) throws SQLException {
        return new PublishStepRecord(
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

    private static PublishRollbackRecord mapRollback(ResultSet rs, int rowNum) throws SQLException {
        return new PublishRollbackRecord(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getString("rollback_target_version"),
                rs.getString("rollback_reason"),
                rs.getString("rollback_status"),
                toInstant(rs.getTimestamp("rolled_back_at")));
    }

    private static Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
