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

overridden_catalog="$(CDD_GATEWAY_SERVER_PORT=19080 CDD_AUTH_SERVER_PORT=19081 backend_runtime_service_catalog)"
assert_equals $'gateway|cdd-gateway|19080|run_gateway.sh\nauth-service|cdd-auth-service|19081|run_auth_service_mysql.sh\nmerchant-service|cdd-merchant-service|8082|run_merchant_service_mysql.sh\ndecoration-service|cdd-decoration-service|8083|run_decoration_service_mysql.sh\nproduct-service|cdd-product-service|8084|run_product_service_mysql.sh\norder-service|cdd-order-service|8085|run_order_service_mysql.sh\nmarketing-service|cdd-marketing-service|8086|run_marketing_service_mysql.sh\nrelease-service|cdd-release-service|8087|run_release_service_mysql.sh\nreport-service|cdd-report-service|8088|run_report_service_mysql.sh\nconfig-service|cdd-config-service|8089|run_config_service_mysql.sh' "$overridden_catalog" "runtime service catalog port overrides"
assert_equals "19080" "$(CDD_GATEWAY_SERVER_PORT=19080 backend_runtime_service_port gateway)" "gateway launcher port resolution"
assert_equals "19081" "$(CDD_AUTH_SERVER_PORT=19081 backend_runtime_service_port auth-service)" "auth launcher port resolution"
assert_fails "duplicate resolved service ports must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_GATEWAY_SERVER_PORT=19080 CDD_AUTH_SERVER_PORT=19080 backend_runtime_service_catalog"
assert_fails "non-numeric service ports must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_GATEWAY_SERVER_PORT=invalid backend_runtime_service_catalog"
assert_fails "out-of-range service ports must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_GATEWAY_SERVER_PORT=65536 backend_runtime_service_catalog"
assert_fails "arbitrarily large service ports must be rejected before arithmetic" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_GATEWAY_SERVER_PORT=18446744073709551617 backend_runtime_service_catalog"
assert_fails "leading-zero service ports must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; CDD_GATEWAY_SERVER_PORT=08080 backend_runtime_service_catalog"

valid_ss_rows=$'LISTEN 0 128 127.0.0.1:8080 0.0.0.0:* users:(("java",pid=123,fd=9))\nLISTEN 0 128 [::1]:8080 [::]:* users:(("java",pid=123,fd=10))'
assert_equals "123" "$(printf '%s\n' "$valid_ss_rows" | backend_runtime_parse_ss_listener_pids 8080 | sort -u)" "ss listener PID normalization"
mixed_ss_rows=$'LISTEN 0 128 127.0.0.1:8080 0.0.0.0:* users:(("java",pid=123,fd=9))\nLISTEN 0 128 [::1]:8080 [::]:*'
assert_fails "every matching ss listener row must expose PID proof" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; printf '%s\n' \"\$1\" | backend_runtime_parse_ss_listener_pids 8080" _ "$mixed_ss_rows"

expected_java_path="/opt/jdk/bin/java"
expected_jar_path="/srv/cdd/cdd-gateway-0.1.0-SNAPSHOT.jar"
assert_equals "" "$(printf '%s\n' "$expected_java_path" -jar "$expected_jar_path" --server.port=8080 --spring.profiles.active=local,file | backend_runtime_java_argv_matches "$expected_java_path" "$expected_jar_path" 8080)" "exact Java argv acceptance"
assert_fails "server port argv must not use prefix matching" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; printf '%s\n' '$expected_java_path' -jar '$expected_jar_path' --server.port=80800 | backend_runtime_java_argv_matches '$expected_java_path' '$expected_jar_path' 8080"
assert_fails "jar argv must not use prefix matching" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; printf '%s\n' '$expected_java_path' -jar '${expected_jar_path}.bak' --server.port=8080 | backend_runtime_java_argv_matches '$expected_java_path' '$expected_jar_path' 8080"
assert_fails "duplicate conflicting server port argv must be rejected" bash -c "source '$repo_root/scripts/local/backend_runtime_guard.sh'; printf '%s\n' '$expected_java_path' -jar '$expected_jar_path' --server.port=8080 --server.port=9999 | backend_runtime_java_argv_matches '$expected_java_path' '$expected_jar_path' 8080"

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
