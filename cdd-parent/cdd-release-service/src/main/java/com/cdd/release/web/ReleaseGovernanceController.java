package com.cdd.release.web;

import com.cdd.api.release.model.CreateReleaseTaskRequest;
import com.cdd.api.release.model.ReleaseTaskResponse;
import com.cdd.api.release.model.ReleaseTaskResultSyncRequest;
import com.cdd.api.release.model.ReleaseTaskRollbackRequest;
import com.cdd.api.release.model.ReleaseTaskStatusUpdateRequest;
import com.cdd.api.release.model.ReleaseTaskStepUpdateRequest;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.release.service.ReleaseGovernanceApplicationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/release")
public class ReleaseGovernanceController {

    private final ReleaseGovernanceApplicationService releaseGovernanceApplicationService;

    public ReleaseGovernanceController(ReleaseGovernanceApplicationService releaseGovernanceApplicationService) {
        this.releaseGovernanceApplicationService = releaseGovernanceApplicationService;
    }

    @PostMapping("/tasks")
    public ApiResponse<ReleaseTaskResponse> createTask(@Valid @RequestBody CreateReleaseTaskRequest request) {
        return ApiResponses.success(releaseGovernanceApplicationService.createReleaseTask(request));
    }

    @GetMapping("/tasks/{task_no}")
    public ApiResponse<ReleaseTaskResponse> getTask(@PathVariable("task_no") String taskNo) {
        return ApiResponses.success(releaseGovernanceApplicationService.getReleaseTask(taskNo));
    }

    @PostMapping("/tasks/{task_no}/status")
    public ApiResponse<ReleaseTaskResponse> updateTaskStatus(@PathVariable("task_no") String taskNo,
                                                             @Valid @RequestBody ReleaseTaskStatusUpdateRequest request) {
        return ApiResponses.success(releaseGovernanceApplicationService.updateReleaseTaskStatus(taskNo, request));
    }

    @PostMapping("/tasks/{task_no}/steps")
    public ApiResponse<ReleaseTaskResponse> updateTaskStep(@PathVariable("task_no") String taskNo,
                                                           @Valid @RequestBody ReleaseTaskStepUpdateRequest request) {
        return ApiResponses.success(releaseGovernanceApplicationService.updateReleaseTaskStep(taskNo, request));
    }

    @PostMapping("/tasks/{task_no}/sync")
    public ApiResponse<ReleaseTaskResponse> syncTaskResult(@PathVariable("task_no") String taskNo,
                                                           @RequestBody(required = false) ReleaseTaskResultSyncRequest request) {
        return ApiResponses.success(releaseGovernanceApplicationService.syncReleaseResult(taskNo, request));
    }

    @PostMapping("/tasks/{task_no}/rollback")
    public ApiResponse<ReleaseTaskResponse> createRollbackTask(@PathVariable("task_no") String taskNo,
                                                               @Valid @RequestBody ReleaseTaskRollbackRequest request) {
        return ApiResponses.success(releaseGovernanceApplicationService.createRollbackTask(taskNo, request));
    }
}
