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
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
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
