#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
state_dir="$repo_root/.local/backend-runtime"
state_file="$state_dir/gateway.env"
backup_file="$(mktemp /tmp/chengdd-gateway-state.XXXXXX)"
had_state=0

cleanup() {
  local code=$?
  if [[ "$had_state" -eq 1 ]]; then
    cp "$backup_file" "$state_file"
  else
    rm -f "$state_file"
  fi
  rm -f "$backup_file"
  if [[ -n "${unrelated_pid:-}" ]] && kill -0 "$unrelated_pid" >/dev/null 2>&1; then
    kill "$unrelated_pid" >/dev/null 2>&1 || true
  fi
  exit "$code"
}
trap cleanup EXIT

mkdir -p "$state_dir/logs"
if [[ -f "$state_file" ]]; then
  cp "$state_file" "$backup_file"
  had_state=1
fi

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

if CDD_ENV=local CDD_CONFIG_MODE=file CDD_RUNTIME_STOP_TIMEOUT_SECONDS=1 bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1; then
  echo "Assertion failed: an unowned process state must make shutdown fail safely." >&2
  exit 1
fi
if ! kill -0 "$unrelated_pid" >/dev/null 2>&1; then
  echo "Assertion failed: shutdown terminated an unrelated process." >&2
  exit 1
fi

echo "runtime stop safety checks passed"
