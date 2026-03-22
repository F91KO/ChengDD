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
