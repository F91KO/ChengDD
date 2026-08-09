#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-status.XXXXXX)"
fixture_bin="$fixture_root/bin"
trace_file="$fixture_root/trace"
mkdir -p "$fixture_bin"

cleanup() {
  local code=$?
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'request="$*"' \
  'if [[ "$request" == *"/actuator/health"* ]]; then' \
  '  [[ "$request" == *":${CDD_TEST_HEALTHY_PORT:-0}/actuator/health"* ]] && exit 0' \
  '  exit 1' \
  'fi' \
  'if [[ "$request" == *"/nacos/v1/cs/configs"* ]]; then printf shared; else printf "{\"hosts\":[]}"; fi' >"$fixture_bin/curl"
chmod +x "$fixture_bin/curl"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$2" >>"$CDD_TEST_TRACE"' 'exit 0' >"$fixture_root/check.sh"
chmod +x "$fixture_root/check.sh"

run_status() {
  env PATH="$fixture_bin:$PATH" \
    CDD_ENV=local \
    CDD_CONFIG_MODE=nacos \
    CDD_RUNTIME_NACOS_CHECK_SCRIPT="$fixture_root/check.sh" \
    CDD_TEST_TRACE="$trace_file" \
    "$@" bash "$repo_root/scripts/local/status_all_services.sh"
}

: >"$trace_file"
if run_status CDD_TEST_HEALTHY_PORT=8080; then
  echo "Assertion failed: partially healthy runtime must be nonzero." >&2
  exit 1
fi
[[ "$(<"$trace_file")" == "running" ]] || {
  echo "Assertion failed: healthy runtime must select Nacos running state." >&2
  exit 1
}

: >"$trace_file"
run_status CDD_TEST_HEALTHY_PORT=0
[[ "$(<"$trace_file")" == "stopped" ]] || {
  echo "Assertion failed: stopped runtime must select Nacos stopped state." >&2
  exit 1
}

echo "runtime status behavior checks passed"
