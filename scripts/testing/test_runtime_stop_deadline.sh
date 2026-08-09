#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-stop-deadline.XXXXXX)"
trace_file="$fixture_root/trace"
state_dir="$fixture_root/runtime-state"
fixture_bin="$fixture_root/bin"
term_trace="$fixture_root/term.trace"
stop_log="$fixture_root/stop.log"
mkdir -p "$state_dir" "$fixture_bin"

cleanup() {
  local code=$?
  for fixture_pid in "${gateway_pid:-}" "${auth_pid:-}" "${unrelated_pid:-}"; do
    [[ -n "$fixture_pid" ]] || continue
    kill -KILL "$fixture_pid" >/dev/null 2>&1 || true
  done
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

write_state() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local service_pid="$4"
  local process_marker="$5"
  printf '%s\n' \
    "SERVICE_NAME=${service_name}" \
    "MODULE_NAME=${module_name}" \
    "SERVICE_PORT=${service_port}" \
    "SERVICE_PID=${service_pid}" \
    "PROCESS_START_MARKER=${process_marker}" \
    'JAVA_PATH=/fixture/java' \
    "JAR_PATH=${repo_root}/cdd-parent/${module_name}/target/${module_name}-0.1.0-SNAPSHOT.jar" \
    'GIT_HEAD=fixture' \
    'BACKEND_FINGERPRINT=fixture' \
    'STARTED_AT=0' \
    'STARTED_AT_TEXT=fixture' >"$state_dir/${service_name}.env"
}

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'request="$*"' \
  'pid=""' \
  'previous=""' \
  'for argument in "$@"; do' \
  '  if [[ "$previous" == "-p" ]]; then pid="$argument"; fi' \
  '  previous="$argument"' \
  'done' \
  'if [[ "$request" == *"lstart="* ]]; then printf "marker-%s\\n" "$pid"; exit 0; fi' \
  'if [[ "$request" == *"state="* ]]; then printf "T\\n"; exit 0; fi' \
  'if [[ "$request" == *"command="* && "$pid" == "$CDD_TEST_GATEWAY_PID" ]]; then printf "/fixture/java -jar %s/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar --server.port=8080\\n" "$CDD_TEST_REPO_ROOT"; exit 0; fi' \
  'if [[ "$request" == *"command="* && "$pid" == "$CDD_TEST_AUTH_PID" ]]; then printf "/fixture/java -jar %s/cdd-parent/cdd-auth-service/target/cdd-auth-service-0.1.0-SNAPSHOT.jar --server.port=8081\\n" "$CDD_TEST_REPO_ROOT"; exit 0; fi' \
  'if [[ "$request" == *"-ax"* ]]; then exit 0; fi' \
  'exit 1' >"$fixture_bin/ps"
chmod +x "$fixture_bin/ps"

bash -c 'trap '\''printf "gateway\\n" >>"$1"'\'' TERM; while :; do sleep 1; done' _ "$term_trace" &
gateway_pid=$!
bash -c 'trap '\''printf "auth\\n" >>"$1"'\'' TERM; while :; do sleep 1; done' _ "$term_trace" &
auth_pid=$!
sleep 30 &
unrelated_pid=$!
write_state gateway cdd-gateway 8080 "$gateway_pid" "marker-${gateway_pid}"
write_state auth-service cdd-auth-service 8081 "$auth_pid" "marker-${auth_pid}"

local_started_at="$(date +%s)"
if ! PATH="$fixture_bin:$PATH" \
  CDD_TEST_GATEWAY_PID="$gateway_pid" \
  CDD_TEST_AUTH_PID="$auth_pid" \
  CDD_TEST_REPO_ROOT="$repo_root" \
  CDD_ENV=local \
  CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_STOP_TIMEOUT_SECONDS=3 \
  CDD_RUNTIME_TERM_GRACE_SECONDS=2 \
  bash "$repo_root/scripts/local/stop_all_services.sh" >"$stop_log" 2>&1; then
  tail -n 160 "$stop_log" >&2
  echo "Assertion failed: phased global shutdown failed to stop all controllable owned processes." >&2
  exit 1
fi
local_elapsed_seconds=$(( $(date +%s) - local_started_at ))
if (( local_elapsed_seconds > 3 )); then
  echo "Assertion failed: process shutdown used per-service deadlines (${local_elapsed_seconds}s)." >&2
  exit 1
fi
if ! kill -0 "$unrelated_pid" >/dev/null 2>&1; then
  echo "Assertion failed: global shutdown deadline killed an unrelated process." >&2
  exit 1
fi
for stopped_pid in "$gateway_pid" "$auth_pid"; do
  if kill -0 "$stopped_pid" >/dev/null 2>&1; then
    echo "Assertion failed: global shutdown left a controllable owned process unsignalled: ${stopped_pid}." >&2
    exit 1
  fi
done
for service_name in gateway auth; do
  rg -Fx "$service_name" "$term_trace" >/dev/null || {
    echo "Assertion failed: phased shutdown did not promptly TERM ${service_name}." >&2
    exit 1
  }
done

rm -rf "$state_dir"
mkdir -p "$state_dir"

printf '%s\n' '#!/usr/bin/env bash' 'echo stale >>"$CDD_TEST_TRACE"' 'exit 1' >"$fixture_root/check.sh"
chmod +x "$fixture_root/check.sh"

started_at="$(date +%s)"
if CDD_ENV=local CDD_CONFIG_MODE=nacos CDD_RUNTIME_STATE_DIR="$state_dir" CDD_RUNTIME_STOP_TIMEOUT_SECONDS=1 CDD_RUNTIME_NACOS_CHECK_SCRIPT="$fixture_root/check.sh" CDD_TEST_TRACE="$trace_file" bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1; then
  echo "Assertion failed: stale Nacos registration must fail shutdown." >&2
  exit 1
fi
elapsed_seconds=$(( $(date +%s) - started_at ))
if (( elapsed_seconds > 4 )); then
  echo "Assertion failed: shutdown exceeded its bounded lifecycle deadline." >&2
  exit 1
fi
[[ -s "$trace_file" ]] || {
  echo "Assertion failed: shutdown did not poll Nacos for stale registrations." >&2
  exit 1
}

echo "runtime stop deadline checks passed"
