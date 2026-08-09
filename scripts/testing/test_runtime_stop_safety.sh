#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-stop-safety.XXXXXX)"
state_dir="$fixture_root/runtime-state"
state_file="$state_dir/gateway.env"

cleanup() {
  local code=$?
  if [[ -n "${unrelated_pid:-}" ]] && kill -0 "$unrelated_pid" >/dev/null 2>&1; then
    kill "$unrelated_pid" >/dev/null 2>&1 || true
  fi
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

mkdir -p "$state_dir/logs"

sleep 30 &
unrelated_pid=$!
printf '%s\n' \
  'SERVICE_NAME=gateway' \
  'MODULE_NAME=cdd-gateway' \
  'SERVICE_PORT=8080' \
  "SERVICE_PID=${unrelated_pid}" \
  'PROCESS_START_MARKER=not-the-sleep-process' \
  'GIT_HEAD=fixture' \
  'BACKEND_FINGERPRINT=fixture' \
  'STARTED_AT=0' \
  'STARTED_AT_TEXT=fixture' >"$state_file"

if CDD_ENV=local CDD_CONFIG_MODE=file CDD_RUNTIME_STATE_DIR="$state_dir" CDD_RUNTIME_STOP_TIMEOUT_SECONDS=1 bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1; then
  echo "Assertion failed: an unowned process state must make shutdown fail safely." >&2
  exit 1
fi
if ! kill -0 "$unrelated_pid" >/dev/null 2>&1; then
  echo "Assertion failed: shutdown terminated an unrelated process." >&2
  exit 1
fi

echo "runtime stop safety checks passed"
