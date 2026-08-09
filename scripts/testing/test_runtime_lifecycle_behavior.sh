#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-lifecycle.XXXXXX)"
trace_file="$fixture_root/trace"
fixture_bin="$fixture_root/bin"
fixture_scripts="$fixture_root/scripts"
fixture_launchers="$fixture_root/launchers"
fixture_state_dir="$fixture_root/runtime-state"
mkdir -p "$fixture_bin" "$fixture_scripts" "$fixture_launchers" "$fixture_state_dir"

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
write_fixture "$fixture_bin/lsof" '#!/usr/bin/env bash' 'exit 1'
write_fixture "$fixture_bin/curl" '#!/usr/bin/env bash' 'for argument in "$@"; do' '  if [[ "$argument" == *":${CDD_TEST_FAIL_HEALTH_PORT:-0}/actuator/health" ]]; then exit 1; fi' 'done' 'exit 0'
write_fixture "$fixture_bin/ps" \
  '#!/usr/bin/env bash' \
  'request="$*"' \
  'pid=""' \
  'previous=""' \
  'for argument in "$@"; do if [[ "$previous" == "-p" ]]; then pid="$argument"; fi; previous="$argument"; done' \
  'if [[ "$request" == *"lstart="* ]]; then printf "fixture-marker-%s\n" "$pid"; exit 0; fi' \
  'if [[ "$request" == *"state="* ]]; then printf "T\n"; exit 0; fi' \
  'if [[ "$request" == *"command="* ]]; then' \
  '  for state_file in "$CDD_RUNTIME_STATE_DIR"/*.env; do' \
  '    [[ -f "$state_file" ]] || continue' \
  '    state_pid="$(awk -F= '\''$1 == "SERVICE_PID" { print $2 }'\'' "$state_file")"' \
  '    [[ "$state_pid" == "$pid" ]] || continue' \
  '    java_path="$(awk -F= '\''$1 == "JAVA_PATH" { print $2 }'\'' "$state_file")"' \
  '    jar_path="$(awk -F= '\''$1 == "JAR_PATH" { print $2 }'\'' "$state_file")"' \
  '    service_port="$(awk -F= '\''$1 == "SERVICE_PORT" { print $2 }'\'' "$state_file")"' \
  '    printf "%s -jar %s --server.port=%s\n" "$java_path" "$jar_path" "$service_port"' \
  '    exit 0' \
  '  done' \
  '  for launcher_state in "$CDD_RUNTIME_STATE_DIR"/logs/*.launcher.env; do' \
  '    [[ -f "$launcher_state" ]] || continue' \
  '    launcher_pid="$(awk -F= '\''$1 == "LAUNCHER_PID" { print $2 }'\'' "$launcher_state")"' \
  '    [[ "$launcher_pid" == "$pid" ]] || continue' \
  '    launcher_name="$(awk -F= '\''$1 == "LAUNCHER_NAME" { print $2 }'\'' "$launcher_state")"' \
  '    printf "bash %s/%s\n" "$CDD_RUNTIME_LAUNCHER_DIR" "$launcher_name"' \
  '    exit 0' \
  '  done' \
  '  if [[ -f "$CDD_RUNTIME_STATE_DIR/logs/launcher.commands" ]]; then' \
  '    while IFS="|" read -r launcher_pid launcher_name; do' \
  '      [[ "$launcher_pid" == "$pid" ]] || continue' \
  '      printf "bash %s/%s\n" "$CDD_RUNTIME_LAUNCHER_DIR" "$launcher_name"' \
  '      exit 0' \
  '    done <"$CDD_RUNTIME_STATE_DIR/logs/launcher.commands"' \
  '  fi' \
  'fi' \
  'exit 1'
write_fixture "$fixture_bin/pgrep" \
  '#!/usr/bin/env bash' \
  'parent_pid="$2"' \
  'for launcher_state in "$CDD_RUNTIME_STATE_DIR"/logs/*.launcher.env; do' \
  '  [[ -f "$launcher_state" ]] || continue' \
  '  launcher_pid="$(awk -F= '\''$1 == "LAUNCHER_PID" { print $2 }'\'' "$launcher_state")"' \
  '  [[ "$launcher_pid" == "$parent_pid" ]] || continue' \
  '  service_name="$(awk -F= '\''$1 == "SERVICE_NAME" { print $2 }'\'' "$launcher_state")"' \
  '  [[ -s "$CDD_RUNTIME_STATE_DIR/logs/${service_name}.child.pid" ]] || exit 1' \
  '  cat "$CDD_RUNTIME_STATE_DIR/logs/${service_name}.child.pid"' \
  '  exit 0' \
  'done' \
  'exit 1'
write_fixture "$fixture_scripts/up.sh" '#!/usr/bin/env bash' 'echo "infra:${CDD_LOCAL_NACOS_CONSOLE_PORT}" >>"$CDD_TEST_TRACE"'
write_fixture "$fixture_scripts/publish.sh" '#!/usr/bin/env bash' 'echo publish >>"$CDD_TEST_TRACE"'
write_fixture "$fixture_scripts/migrate.sh" '#!/usr/bin/env bash' 'echo migrate >>"$CDD_TEST_TRACE"'

for launcher in run_gateway.sh run_auth_service_mysql.sh run_merchant_service_mysql.sh run_decoration_service_mysql.sh run_product_service_mysql.sh run_order_service_mysql.sh run_marketing_service_mysql.sh run_release_service_mysql.sh run_report_service_mysql.sh run_config_service_mysql.sh; do
  case "$launcher" in
    run_gateway.sh) service_name=gateway; module_name=cdd-gateway; port_expression='${CDD_GATEWAY_SERVER_PORT:-8080}' ;;
    run_auth_service_mysql.sh) service_name=auth-service; module_name=cdd-auth-service; port_expression='${CDD_AUTH_SERVER_PORT:-8081}' ;;
    run_merchant_service_mysql.sh) service_name=merchant-service; module_name=cdd-merchant-service; port_expression='${CDD_MERCHANT_SERVER_PORT:-8082}' ;;
    run_decoration_service_mysql.sh) service_name=decoration-service; module_name=cdd-decoration-service; port_expression='${CDD_DECORATION_SERVER_PORT:-8083}' ;;
    run_product_service_mysql.sh) service_name=product-service; module_name=cdd-product-service; port_expression='${CDD_PRODUCT_SERVER_PORT:-8084}' ;;
    run_order_service_mysql.sh) service_name=order-service; module_name=cdd-order-service; port_expression='${CDD_ORDER_SERVER_PORT:-8085}' ;;
    run_marketing_service_mysql.sh) service_name=marketing-service; module_name=cdd-marketing-service; port_expression='${CDD_MARKETING_SERVER_PORT:-8086}' ;;
    run_release_service_mysql.sh) service_name=release-service; module_name=cdd-release-service; port_expression='${CDD_RELEASE_SERVER_PORT:-8087}' ;;
    run_report_service_mysql.sh) service_name=report-service; module_name=cdd-report-service; port_expression='${CDD_REPORT_SERVER_PORT:-8088}' ;;
    run_config_service_mysql.sh) service_name=config-service; module_name=cdd-config-service; port_expression='${CDD_CONFIG_SERVER_PORT:-8089}' ;;
  esac
  write_fixture "$fixture_launchers/$launcher" \
    '#!/usr/bin/env bash' \
    'echo "fixture-launcher:'"$launcher"'"' \
    'echo "launch '"$launcher"'" >>"$CDD_TEST_TRACE"' \
    'printf "%s|%s\n" "$$" "'"$launcher"'" >>"$CDD_RUNTIME_STATE_DIR/logs/launcher.commands"' \
    'service_port="'"$port_expression"'"' \
    'trap "" TERM' \
    'bash -c '\''trap "" TERM; while :; do sleep 1; done'\'' &' \
    'child_pid=$!' \
    'printf "%s\n" "$child_pid" >"$CDD_RUNTIME_STATE_DIR/logs/'"$service_name"'.child.pid"' \
    'cat >"$CDD_RUNTIME_STATE_DIR/'"$service_name"'.env" <<EOF' \
    'SERVICE_NAME='"$service_name" \
    'MODULE_NAME='"$module_name" \
    'SERVICE_PORT=${service_port}' \
    'SERVICE_PID=${child_pid}' \
    'PROCESS_START_MARKER=fixture-marker-${child_pid}' \
    'JAVA_PATH='"$fixture_root"'/java' \
    'JAR_PATH='"$repo_root"'/cdd-parent/'"$module_name"'/target/'"$module_name"'-0.1.0-SNAPSHOT.jar' \
    'GIT_HEAD=fixture' \
    'BACKEND_FINGERPRINT=fixture' \
    'STARTED_AT=0' \
    'STARTED_AT_TEXT=fixture' \
    'EOF' \
    'while :; do sleep 1; done'
done

clear_fixture_runtime() {
  local state_file fixture_pid
  for state_file in "$fixture_state_dir"/*.env; do
    [[ -f "$state_file" ]] || continue
    fixture_pid="$(awk -F= '$1 == "SERVICE_PID" { print $2 }' "$state_file")"
    [[ -n "$fixture_pid" ]] && kill -KILL "$fixture_pid" >/dev/null 2>&1 || true
  done
  for child_file in "$fixture_state_dir"/logs/*.child.pid; do
    [[ -s "$child_file" ]] || continue
    kill -KILL "$(<"$child_file")" >/dev/null 2>&1 || true
  done
  sleep 0.1
  rm -f "$fixture_state_dir"/*.env "$fixture_state_dir"/logs/*.launcher.env "$fixture_state_dir"/logs/*.child.pid "$fixture_state_dir"/logs/launcher.commands
}

run_all() {
  env PATH="$fixture_bin:$PATH" \
    CDD_TEST_TRACE="$trace_file" \
    CDD_RUNTIME_UP_INFRA_SCRIPT="$fixture_scripts/up.sh" \
    CDD_RUNTIME_PUBLISH_SCRIPT="$fixture_scripts/publish.sh" \
    CDD_RUNTIME_MIGRATE_SCRIPT="$fixture_scripts/migrate.sh" \
    CDD_RUNTIME_LAUNCHER_DIR="$fixture_launchers" \
    CDD_RUNTIME_STATE_DIR="$fixture_state_dir" \
    CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS=7 \
    CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS=3 \
    "$@" bash "$repo_root/scripts/local/run_all_services_mysql.sh"
}

run_all CDD_ENV=local CDD_CONFIG_MODE=nacos
expected_nacos_trace=$'infra:18080\npublish\nmigrate\nlaunch run_gateway.sh\nlaunch run_auth_service_mysql.sh\nlaunch run_merchant_service_mysql.sh\nlaunch run_decoration_service_mysql.sh\nlaunch run_product_service_mysql.sh\nlaunch run_order_service_mysql.sh\nlaunch run_marketing_service_mysql.sh\nlaunch run_release_service_mysql.sh\nlaunch run_report_service_mysql.sh\nlaunch run_config_service_mysql.sh'
assert_equals "$expected_nacos_trace" "$(<"$trace_file")" "nacos lifecycle ordering"
[[ -f "$fixture_state_dir/logs/gateway.launcher.env" ]] || {
  echo "Assertion failed: fixture launch state was not written to its isolated root." >&2
  exit 1
}
rg -F 'fixture-launcher:run_gateway.sh' "$fixture_state_dir/logs/gateway.log" >/dev/null || {
  echo "Assertion failed: fixture launcher log was not written to its isolated root." >&2
  exit 1
}
if rg -F 'fixture-launcher:' "$repo_root/.local/backend-runtime/logs" >/dev/null 2>&1; then
  echo "Assertion failed: fixture log leaked into the default runtime root." >&2
  exit 1
fi
while IFS= read -r launcher_state; do
  launcher_pid="$(awk -F= '$1 == "LAUNCHER_PID" { print $2 }' "$launcher_state")"
  if [[ -n "$launcher_pid" ]] && rg -F "LAUNCHER_PID=${launcher_pid}" "$repo_root/.local/backend-runtime" >/dev/null 2>&1; then
    echo "Assertion failed: fixture launcher state leaked into the default runtime root." >&2
    exit 1
  fi
done < <(find "$fixture_state_dir/logs" -name '*.launcher.env' -type f | sort)
clear_fixture_runtime

: >"$trace_file"
run_all CDD_ENV=local CDD_CONFIG_MODE=file
expected_file_trace=$'infra:18080\nmigrate\nlaunch run_gateway.sh\nlaunch run_auth_service_mysql.sh\nlaunch run_merchant_service_mysql.sh\nlaunch run_decoration_service_mysql.sh\nlaunch run_product_service_mysql.sh\nlaunch run_order_service_mysql.sh\nlaunch run_marketing_service_mysql.sh\nlaunch run_release_service_mysql.sh\nlaunch run_report_service_mysql.sh\nlaunch run_config_service_mysql.sh'
assert_equals "$expected_file_trace" "$(<"$trace_file")" "file mode must bypass publication"
clear_fixture_runtime

if run_all CDD_ENV=local CDD_CONFIG_MODE=file CDD_LOCAL_NACOS_CONSOLE_PORT=8080; then
  echo "Assertion failed: gateway and Nacos Console port collision must fail before infrastructure startup." >&2
  exit 1
fi

: >"$trace_file"
if run_all CDD_ENV=local CDD_CONFIG_MODE=file CDD_GATEWAY_SERVER_PORT=19080 CDD_AUTH_SERVER_PORT=19080; then
  echo "Assertion failed: duplicate resolved service ports must fail before infrastructure startup." >&2
  exit 1
fi
[[ ! -s "$trace_file" ]] || {
  echo "Assertion failed: service port validation ran after infrastructure startup." >&2
  exit 1
}

: >"$trace_file"
if run_all CDD_ENV=local CDD_CONFIG_MODE=nacos CDD_AUTH_SERVER_PORT=19081 CDD_TEST_FAIL_HEALTH_PORT=19081; then
  echo "Assertion failed: child launcher failure must fail run-all." >&2
  exit 1
fi
if ! rg -F 'launch run_auth_service_mysql.sh' "$trace_file" >/dev/null; then
  echo "Assertion failed: child failure did not reach the auth launcher." >&2
  exit 1
fi
clear_fixture_runtime

echo "runtime lifecycle behavior checks passed"
