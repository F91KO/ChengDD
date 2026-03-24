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

export interface ConfigPublishConfigValueResponseRaw {
  config_group: string;
  config_key: string;
  config_value: string;
  config_desc: string | null;
  source: string;
  merchant_id: string | null;
}

export interface ConfigPublishStepResponseRaw {
  step_code: string;
  step_name: string;
  step_order: number | null;
  step_status: string;
  result_message: string | null;
  error_code: string | null;
  retry_count: number | null;
  started_at: string | null;
  finished_at: string | null;
}

export interface ConfigPublishSnapshotResponseRaw {
  platform_configs: ConfigPublishConfigValueResponseRaw[];
  merchant_overrides: ConfigPublishConfigValueResponseRaw[];
  platform_feature_switches: FeatureSwitchValueResponseRaw[];
  merchant_feature_switches: FeatureSwitchValueResponseRaw[];
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
  snapshot: ConfigPublishSnapshotResponseRaw | null;
  steps: ConfigPublishStepResponseRaw[];
}
