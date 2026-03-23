export interface FeatureSwitchValueResponseRaw {
  switch_code: string;
  switch_name: string;
  switch_scope: string;
  default_value: string;
  effective_value: string;
  status: string;
  source: string;
  merchant_id: string | null;
}

export interface ConfigKvValueResponseRaw {
  config_group: string;
  config_key: string;
  config_value: string;
  source: string;
  merchant_id: string | null;
}

export interface ConfigPublishRecordResponseRaw {
  task_no: string;
  merchant_id: string;
  store_id: string;
  release_type: string;
  release_status: string;
  trigger_source: string;
  operator_name: string;
  publish_note: string;
  rollback_reason: string | null;
  rollback_target_task_no: string | null;
  rollback_task_no: string | null;
  started_at: string | null;
  finished_at: string | null;
  created_at: string;
  config_count: number | null;
  feature_switch_count: number | null;
}
