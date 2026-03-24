export interface ReleaseTaskStepResponseRaw {
  step_code: string;
  step_name: string;
  step_order: number;
  step_status: string;
  result_message: string | null;
  error_code: string | null;
  retry_count: number;
  started_at: string | null;
  finished_at: string | null;
}

export interface ReleaseTaskResponseRaw {
  task_no: string;
  merchant_id: number;
  store_id: number;
  mini_program_id: number;
  template_version_id: number;
  release_type: string;
  release_status: string;
  trigger_source: string;
  current_step_code: string | null;
  result_sync_status: string;
  rollback_task_no: string | null;
  last_error_code: string | null;
  last_error_message: string | null;
  started_at: string | null;
  finished_at: string | null;
  steps: ReleaseTaskStepResponseRaw[];
}
