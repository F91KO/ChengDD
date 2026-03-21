package com.cdd.release.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cdd.api.release.model.CreateReleaseTaskRequest;
import com.cdd.api.release.model.ReleaseTaskResponse;
import com.cdd.api.release.model.ReleaseTaskResultSyncRequest;
import com.cdd.api.release.model.ReleaseTaskRollbackRequest;
import com.cdd.api.release.model.ReleaseTaskStatusUpdateRequest;
import com.cdd.api.release.model.ReleaseTaskStepUpdateRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.release.infrastructure.persistence.JdbcReleaseGovernanceRepository;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository;
import com.cdd.release.support.IdGenerator;
import com.cdd.release.support.TimeBasedIdGenerator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

class ReleaseGovernanceApplicationServiceTest {

    private ReleaseGovernanceApplicationService service;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:cdd-release-test-" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        DatabasePopulatorUtils.execute(new ResourceDatabasePopulator(
                new ClassPathResource("db/release/h2/schema.sql"),
                new ClassPathResource("db/release/h2/data-test.sql")), dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        ReleaseGovernanceRepository repository = new JdbcReleaseGovernanceRepository(jdbcTemplate);
        IdGenerator idGenerator = new TimeBasedIdGenerator();
        this.service = new ReleaseGovernanceApplicationService(repository, idGenerator);
    }

    @Test
    void shouldCompleteReleaseFlowAndSyncResult() {
        ReleaseTaskResponse created = service.createReleaseTask(createRequest());
        assertEquals("pending", created.releaseStatus());

        service.updateReleaseTaskStatus(created.taskNo(), new ReleaseTaskStatusUpdateRequest("running", "upload_code", null, null));
        service.updateReleaseTaskStep(created.taskNo(), new ReleaseTaskStepUpdateRequest(
                "upload_code", "上传代码", 2, "success", "上传成功", null));
        service.updateReleaseTaskStatus(created.taskNo(), new ReleaseTaskStatusUpdateRequest("success", "release_done", null, null));

        ReleaseTaskResponse synced = service.syncReleaseResult(created.taskNo(), new ReleaseTaskResultSyncRequest("active"));
        assertEquals("synced", synced.resultSyncStatus());

        String currentTemplateVersion = jdbcTemplate.queryForObject("""
                SELECT current_template_version
                FROM cdd_merchant_mini_program
                WHERE id = 30001
                """, String.class);
        assertEquals("1.0.0", currentTemplateVersion);

        Integer mappingCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_release_version_mapping
                WHERE mini_program_id = 30001
                  AND template_version_id = 40001
                  AND mapping_status = 'active'
                  AND deleted = 0
                """, Integer.class);
        assertEquals(1, mappingCount);
    }

    @Test
    void shouldMarkTaskFailedAndIncreaseRetryCountWhenStepFailsRepeatedly() {
        ReleaseTaskResponse created = service.createReleaseTask(createRequest());

        ReleaseTaskResponse firstFailed = service.updateReleaseTaskStep(created.taskNo(), new ReleaseTaskStepUpdateRequest(
                "validate_env", "环境校验", 2, "failed", "环境校验超时", "ENV_TIMEOUT"));
        assertEquals("failed", firstFailed.releaseStatus());
        assertEquals(1, findStepRetryCount(created.taskNo(), "validate_env"));

        service.updateReleaseTaskStep(created.taskNo(), new ReleaseTaskStepUpdateRequest(
                "validate_env", "环境校验", 2, "failed", "环境校验超时", "ENV_TIMEOUT"));
        assertEquals(2, findStepRetryCount(created.taskNo(), "validate_env"));
    }

    @Test
    void shouldCreateRollbackSkeletonTaskAndRecord() {
        ReleaseTaskResponse created = service.createReleaseTask(createRequest());
        service.updateReleaseTaskStatus(created.taskNo(), new ReleaseTaskStatusUpdateRequest("running", "prepare", null, null));
        service.updateReleaseTaskStatus(created.taskNo(), new ReleaseTaskStatusUpdateRequest("success", "done", null, null));

        ReleaseTaskResponse rollbacking = service.createRollbackTask(
                created.taskNo(),
                new ReleaseTaskRollbackRequest("0.9.0", "线上故障回滚"));
        assertEquals("rolling_back", rollbacking.releaseStatus());
        assertNotNull(rollbacking.rollbackTaskNo());

        Integer rollbackTaskCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_release_task
                WHERE task_no = ?
                  AND release_type = 'rollback'
                  AND deleted = 0
                """, Integer.class, rollbacking.rollbackTaskNo());
        assertEquals(1, rollbackTaskCount);

        Long sourceTaskId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM cdd_release_task
                WHERE task_no = ?
                """, Long.class, created.taskNo());
        Integer rollbackRecordCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_release_rollback_record
                WHERE task_id = ?
                  AND deleted = 0
                """, Integer.class, sourceTaskId);
        assertEquals(1, rollbackRecordCount);
    }

    @Test
    void shouldRejectSyncWhenTaskIsNotSuccessful() {
        ReleaseTaskResponse created = service.createReleaseTask(createRequest());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.syncReleaseResult(created.taskNo(), new ReleaseTaskResultSyncRequest("active")));
        assertEquals("仅成功状态任务允许回写发布结果", ex.getMessage());
    }

    private CreateReleaseTaskRequest createRequest() {
        return new CreateReleaseTaskRequest(
                10001L,
                20001L,
                30001L,
                40001L,
                "full_release",
                "manual",
                "{\"version\":\"1.0.0\"}");
    }

    private int findStepRetryCount(String taskNo, String stepCode) {
        Integer retryCount = jdbcTemplate.queryForObject("""
                SELECT d.retry_count
                FROM cdd_release_task_detail d
                JOIN cdd_release_task t ON t.id = d.task_id
                WHERE t.task_no = ?
                  AND d.step_code = ?
                  AND d.deleted = 0
                LIMIT 1
                """, Integer.class, taskNo, stepCode);
        return retryCount == null ? 0 : retryCount;
    }
}
