#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

assert_equals() {
  local expected="$1"
  local actual="$2"
  local message="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "Assertion failed: ${message}. Expected '${expected}', got '${actual}'." >&2
    exit 1
  fi
}

assert_fails() {
  local message="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "Assertion failed: ${message}." >&2
    exit 1
  fi
}

assert_runtime_configuration() {
  local env_value="$1"
  local mode_value="$2"
  local expected_env="$3"
  local expected_mode="$4"
  (
    CDD_ENV="$env_value"
    CDD_CONFIG_MODE="$mode_value"
    configure_backend_runtime
    assert_equals "$expected_env" "$runtime_env" "runtime environment"
    assert_equals "$expected_mode" "$runtime_config_mode" "runtime configuration mode"
  )
}

assert_runtime_configuration "local" "file" "local" "file"
assert_runtime_configuration "prod" "nacos" "prod" "nacos"
assert_fails "invalid environment must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_ENV=stage CDD_CONFIG_MODE=file configure_backend_runtime"
assert_fails "invalid configuration mode must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_ENV=local CDD_CONFIG_MODE=remote configure_backend_runtime"

expected_catalog=$'gateway|cdd-gateway|8080|run_gateway.sh\nauth-service|cdd-auth-service|8081|run_auth_service_mysql.sh\nmerchant-service|cdd-merchant-service|8082|run_merchant_service_mysql.sh\ndecoration-service|cdd-decoration-service|8083|run_decoration_service_mysql.sh\nproduct-service|cdd-product-service|8084|run_product_service_mysql.sh\norder-service|cdd-order-service|8085|run_order_service_mysql.sh\nmarketing-service|cdd-marketing-service|8086|run_marketing_service_mysql.sh\nrelease-service|cdd-release-service|8087|run_release_service_mysql.sh\nreport-service|cdd-report-service|8088|run_report_service_mysql.sh\nconfig-service|cdd-config-service|8089|run_config_service_mysql.sh'
assert_equals "$expected_catalog" "$(backend_runtime_service_catalog)" "runtime service catalog"

while IFS='|' read -r _service_name _service_module _service_port launcher; do
  launcher_path="$repo_root/scripts/local/$launcher"
  [[ -f "$launcher_path" ]] || {
    echo "Missing service launcher: ${launcher}" >&2
    exit 1
  }
  rg -F -- 'source "$repo_root/scripts/local/run_packaged_module.sh"' "$launcher_path" >/dev/null
done < <(backend_runtime_service_catalog)
rg -F -- 'CDD_MERCHANT_DB_URL' "$repo_root/scripts/local/run_merchant_service_mysql.sh" >/dev/null
rg -F -- 'CDD_MERCHANT_DB_USERNAME' "$repo_root/scripts/local/run_merchant_service_mysql.sh" >/dev/null
rg -F -- 'CDD_MERCHANT_DB_PASSWORD' "$repo_root/scripts/local/run_merchant_service_mysql.sh" >/dev/null

rg -F -- '--spring.profiles.active="${runtime_env},${runtime_config_mode}"' "$repo_root/scripts/local/run_packaged_module.sh" >/dev/null
rg -F -- 'publish_nacos_configs.sh" "$runtime_env"' "$repo_root/scripts/local/run_packaged_module.sh" >/dev/null
if rg -n 'CDD_NACOS_FAIL_FAST|CDD_NACOS_REQUIRE_|CDD_NACOS_DISCOVERY_ENABLED' "$repo_root/scripts/local" "$repo_root/scripts/nacos" >/dev/null; then
  echo "Removed custom Nacos switches are still present." >&2
  exit 1
fi

for lifecycle_script in run_all_services_mysql.sh status_all_services.sh stop_all_services.sh; do
  [[ -f "$repo_root/scripts/local/$lifecycle_script" ]] || {
    echo "Missing lifecycle script: ${lifecycle_script}" >&2
    exit 1
  }
  rg -F -- 'backend_runtime_service_catalog' "$repo_root/scripts/local/$lifecycle_script" >/dev/null
done
rg -F -- 'runtime_nacos_checker_script="${CDD_RUNTIME_NACOS_CHECK_SCRIPT:-$repo_root/scripts/nacos/check_nacos_state.sh}"' "$repo_root/scripts/local/status_all_services.sh" >/dev/null
rg -F -- 'bash "$runtime_nacos_checker_script" "$runtime_env" running' "$repo_root/scripts/local/status_all_services.sh" >/dev/null
rg -F -- 'bash "$runtime_nacos_checker_script" "$runtime_env" stopped' "$repo_root/scripts/local/status_all_services.sh" >/dev/null
rg -F -- 'CDD_NACOS_DEADLINE_EPOCH="$nacos_stop_deadline" bash "$nacos_checker_script" "$runtime_env" stopped' "$repo_root/scripts/local/stop_all_services.sh" >/dev/null
rg -F -- 'remove_backend_runtime_state' "$repo_root/scripts/local/stop_all_services.sh" >/dev/null

echo "runtime profile contract checks passed"
