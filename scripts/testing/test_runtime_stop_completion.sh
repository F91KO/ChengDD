#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-stop-completion.XXXXXX)"
fixture_bin="$fixture_root/bin"
state_dir="$fixture_root/runtime-state"
mkdir -p "$fixture_bin" "$state_dir"

cleanup() {
  local code=$?
  if [[ -n "${service_pid:-}" ]] && kill -0 "$service_pid" >/dev/null 2>&1; then
    kill -KILL "$service_pid" >/dev/null 2>&1 || true
  fi
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'request="$*"' \
  'if [[ "$request" == *"lstart="* ]]; then printf "%s\\n" "${CDD_TEST_PROCESS_MARKER}"; exit 0; fi' \
  'if [[ "$request" == *"-ax"* ]]; then exit 0; fi' \
  'if [[ "$request" == *"command="* ]]; then printf "%s\\n" "/fixture/java -jar '"$repo_root"'/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar --server.port=8080"; exit 0; fi' \
  'exit 0' >"$fixture_bin/ps"
chmod +x "$fixture_bin/ps"

bash -c 'trap "" TERM; while :; do sleep 1; done' &
service_pid=$!
process_marker="$(PATH="$fixture_bin:$PATH" CDD_TEST_PROCESS_MARKER=fixture-marker bash -c 'source "$1"; backend_runtime_process_start_marker "$2"' _ "$repo_root/scripts/local/backend_runtime_guard.sh" "$service_pid")"
[[ -n "$process_marker" ]] || {
  echo "Assertion failed: fixture process marker was not available." >&2
  exit 1
}

printf '%s\n' \
  'SERVICE_NAME=gateway' \
  'MODULE_NAME=cdd-gateway' \
  'SERVICE_PORT=8080' \
  "SERVICE_PID=${service_pid}" \
  "PROCESS_START_MARKER=${process_marker}" \
  'JAVA_PATH=/fixture/java' \
  "JAR_PATH=${repo_root}/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar" \
  'GIT_HEAD=fixture' \
  'BACKEND_FINGERPRINT=fixture' \
  'STARTED_AT=0' \
  'STARTED_AT_TEXT=fixture' >"$state_dir/gateway.env"

PATH="$fixture_bin:$PATH" CDD_TEST_PROCESS_MARKER=fixture-marker CDD_RUNTIME_STATE_DIR="$state_dir" CDD_RUNTIME_STOP_TIMEOUT_SECONDS=4 CDD_RUNTIME_TERM_GRACE_SECONDS=1 CDD_ENV=local CDD_CONFIG_MODE=file bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null
if kill -0 "$service_pid" >/dev/null 2>&1; then
  echo "Assertion failed: owned TERM-resistant process was not force stopped." >&2
  exit 1
fi
[[ ! -e "$state_dir/gateway.env" ]] || {
  echo "Assertion failed: completed shutdown did not remove runtime state." >&2
  exit 1
}

echo "runtime stop completion checks passed"
