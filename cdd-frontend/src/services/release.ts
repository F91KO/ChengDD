import { requestApi } from '@/services/http';
import type { ReleaseTaskResponseRaw } from '@/types/release';

export async function createReleaseTask(payload: {
  merchantId: number;
  storeId: number;
  miniProgramId: number;
  templateVersionId: number;
  releaseType: string;
  triggerSource: string;
  releaseSnapshotJson?: string;
}): Promise<ReleaseTaskResponseRaw> {
  return requestApi<ReleaseTaskResponseRaw>({
    method: 'POST',
    url: '/release/tasks',
    data: {
      mini_program_id: payload.miniProgramId,
      template_version_id: payload.templateVersionId,
      release_type: payload.releaseType,
      trigger_source: payload.triggerSource,
      release_snapshot_json: payload.releaseSnapshotJson ?? '',
    },
  });
}

export async function fetchReleaseTask(taskNo: string): Promise<ReleaseTaskResponseRaw> {
  return requestApi<ReleaseTaskResponseRaw>({
    method: 'GET',
    url: `/release/tasks/${taskNo}`,
  });
}

export async function updateReleaseTaskStatus(payload: {
  taskNo: string;
  targetStatus: string;
  currentStepCode?: string;
  errorCode?: string;
  errorMessage?: string;
}): Promise<ReleaseTaskResponseRaw> {
  return requestApi<ReleaseTaskResponseRaw>({
    method: 'POST',
    url: `/release/tasks/${payload.taskNo}/status`,
    data: {
      target_status: payload.targetStatus,
      current_step_code: payload.currentStepCode ?? '',
      error_code: payload.errorCode ?? '',
      error_message: payload.errorMessage ?? '',
    },
  });
}

export async function updateReleaseTaskStep(payload: {
  taskNo: string;
  stepCode: string;
  stepName: string;
  stepOrder: number;
  stepStatus: string;
  resultMessage?: string;
  errorCode?: string;
}): Promise<ReleaseTaskResponseRaw> {
  return requestApi<ReleaseTaskResponseRaw>({
    method: 'POST',
    url: `/release/tasks/${payload.taskNo}/steps`,
    data: {
      step_code: payload.stepCode,
      step_name: payload.stepName,
      step_order: payload.stepOrder,
      step_status: payload.stepStatus,
      result_message: payload.resultMessage ?? '',
      error_code: payload.errorCode ?? '',
    },
  });
}

export async function syncReleaseTaskResult(payload: {
  taskNo: string;
  mappingStatus: string;
}): Promise<ReleaseTaskResponseRaw> {
  return requestApi<ReleaseTaskResponseRaw>({
    method: 'POST',
    url: `/release/tasks/${payload.taskNo}/sync`,
    data: {
      mapping_status: payload.mappingStatus,
    },
  });
}

export async function rollbackReleaseTask(payload: {
  taskNo: string;
  rollbackTargetVersion: string;
  rollbackReason: string;
}): Promise<ReleaseTaskResponseRaw> {
  return requestApi<ReleaseTaskResponseRaw>({
    method: 'POST',
    url: `/release/tasks/${payload.taskNo}/rollback`,
    data: {
      rollback_target_version: payload.rollbackTargetVersion,
      rollback_reason: payload.rollbackReason,
    },
  });
}
