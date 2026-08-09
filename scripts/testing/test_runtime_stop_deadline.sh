#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-stop-deadline.XXXXXX)"
trace_file="$fixture_root/trace"

cleanup() {
  local code=$?
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

printf '%s\n' '#!/usr/bin/env bash' 'echo stale >>"$CDD_TEST_TRACE"' 'exit 1' >"$fixture_root/check.sh"
chmod +x "$fixture_root/check.sh"

started_at="$(date +%s)"
if CDD_ENV=local CDD_CONFIG_MODE=nacos CDD_RUNTIME_STOP_TIMEOUT_SECONDS=1 CDD_RUNTIME_NACOS_CHECK_SCRIPT="$fixture_root/check.sh" CDD_TEST_TRACE="$trace_file" bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1; then
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
