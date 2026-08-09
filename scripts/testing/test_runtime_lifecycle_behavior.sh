#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-lifecycle.XXXXXX)"
trace_file="$fixture_root/trace"
fixture_bin="$fixture_root/bin"
fixture_scripts="$fixture_root/scripts"
fixture_launchers="$fixture_root/launchers"
mkdir -p "$fixture_bin" "$fixture_scripts" "$fixture_launchers"

cleanup() {
  local code=$?
  jobs -pr | xargs -r kill >/dev/null 2>&1 || true
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

assert_equals() {
  local expected="$1"
  local actual="$2"
  local message="$3"
  if [[ "$expected" != "$actual" ]]; then
    echo "Assertion failed: ${message}." >&2
    echo "Expected:" >&2
    printf '%s\n' "$expected" >&2
    echo "Actual:" >&2
    printf '%s\n' "$actual" >&2
    exit 1
  fi
}

write_fixture() {
  local path="$1"
  shift
  printf '%s\n' "$@" >"$path"
  chmod +x "$path"
}

write_fixture "$fixture_bin/docker" '#!/usr/bin/env bash' 'exit 1'
write_fixture "$fixture_bin/curl" '#!/usr/bin/env bash' 'for argument in "$@"; do' '  if [[ "$argument" == *":${CDD_TEST_FAIL_HEALTH_PORT:-0}/actuator/health" ]]; then exit 1; fi' 'done' 'exit 0'
write_fixture "$fixture_scripts/up.sh" '#!/usr/bin/env bash' 'echo "infra:${CDD_LOCAL_NACOS_CONSOLE_PORT}" >>"$CDD_TEST_TRACE"'
write_fixture "$fixture_scripts/publish.sh" '#!/usr/bin/env bash' 'echo publish >>"$CDD_TEST_TRACE"'
write_fixture "$fixture_scripts/migrate.sh" '#!/usr/bin/env bash' 'echo migrate >>"$CDD_TEST_TRACE"'

for launcher in run_gateway.sh run_auth_service_mysql.sh run_merchant_service_mysql.sh run_decoration_service_mysql.sh run_product_service_mysql.sh run_order_service_mysql.sh run_marketing_service_mysql.sh run_release_service_mysql.sh run_report_service_mysql.sh run_config_service_mysql.sh; do
  write_fixture "$fixture_launchers/$launcher" '#!/usr/bin/env bash' 'echo "launch '"$launcher"'" >>"$CDD_TEST_TRACE"' 'sleep 30'
done

run_all() {
  env PATH="$fixture_bin:$PATH" \
    CDD_TEST_TRACE="$trace_file" \
    CDD_RUNTIME_UP_INFRA_SCRIPT="$fixture_scripts/up.sh" \
    CDD_RUNTIME_PUBLISH_SCRIPT="$fixture_scripts/publish.sh" \
    CDD_RUNTIME_MIGRATE_SCRIPT="$fixture_scripts/migrate.sh" \
    CDD_RUNTIME_LAUNCHER_DIR="$fixture_launchers" \
    CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS=1 \
    "$@" bash "$repo_root/scripts/local/run_all_services_mysql.sh"
}

run_all CDD_ENV=local CDD_CONFIG_MODE=nacos
expected_nacos_trace=$'infra:18080\npublish\nmigrate\nlaunch run_gateway.sh\nlaunch run_auth_service_mysql.sh\nlaunch run_merchant_service_mysql.sh\nlaunch run_decoration_service_mysql.sh\nlaunch run_product_service_mysql.sh\nlaunch run_order_service_mysql.sh\nlaunch run_marketing_service_mysql.sh\nlaunch run_release_service_mysql.sh\nlaunch run_report_service_mysql.sh\nlaunch run_config_service_mysql.sh'
assert_equals "$expected_nacos_trace" "$(<"$trace_file")" "nacos lifecycle ordering"

: >"$trace_file"
run_all CDD_ENV=local CDD_CONFIG_MODE=file
expected_file_trace=$'infra:18080\nmigrate\nlaunch run_gateway.sh\nlaunch run_auth_service_mysql.sh\nlaunch run_merchant_service_mysql.sh\nlaunch run_decoration_service_mysql.sh\nlaunch run_product_service_mysql.sh\nlaunch run_order_service_mysql.sh\nlaunch run_marketing_service_mysql.sh\nlaunch run_release_service_mysql.sh\nlaunch run_report_service_mysql.sh\nlaunch run_config_service_mysql.sh'
assert_equals "$expected_file_trace" "$(<"$trace_file")" "file mode must bypass publication"

if run_all CDD_ENV=local CDD_CONFIG_MODE=file CDD_LOCAL_NACOS_CONSOLE_PORT=8080; then
  echo "Assertion failed: gateway and Nacos Console port collision must fail before infrastructure startup." >&2
  exit 1
fi

: >"$trace_file"
if run_all CDD_ENV=local CDD_CONFIG_MODE=nacos CDD_TEST_FAIL_HEALTH_PORT=8081; then
  echo "Assertion failed: child launcher failure must fail run-all." >&2
  exit 1
fi
if ! rg -F 'launch run_auth_service_mysql.sh' "$trace_file" >/dev/null; then
  echo "Assertion failed: child failure did not reach the auth launcher." >&2
  exit 1
fi

echo "runtime lifecycle behavior checks passed"
