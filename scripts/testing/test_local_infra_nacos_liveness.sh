#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-local-infra-liveness.XXXXXX)"
fixture_bin="$fixture_root/bin"
trace_file="$fixture_root/trace"
mkdir -p "$fixture_bin"

cleanup() {
  local cdd_exit_code=$?
  rm -rf "$fixture_root"
  exit "$cdd_exit_code"
}
trap cleanup EXIT

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'printf "docker:%s\\n" "$*" >>"$CDD_TEST_TRACE"' \
  'if [[ "$1" == "compose" && "$2" == "version" ]]; then exit 0; fi' \
  'if [[ "$1" == "compose" && "$*" == *" up -d" ]]; then exit 0; fi' \
  'exit 88' >"$fixture_bin/docker"
chmod +x "$fixture_bin/docker"

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'request="${*: -1}"' \
  'printf "curl:%s\\n" "$request" >>"$CDD_TEST_TRACE"' \
  '[[ "$request" == "http://127.0.0.1:18080/v3/console/health/liveness" ]] || exit 89' \
  'exit 0' >"$fixture_bin/curl"
chmod +x "$fixture_bin/curl"

printf '%s\n' '#!/usr/bin/env bash' 'exit 0' >"$fixture_bin/sleep"
chmod +x "$fixture_bin/sleep"

run_output="$(env PATH="$fixture_bin:$PATH" CDD_TEST_TRACE="$trace_file" CDD_LOCAL_NACOS_CONSOLE_PORT=18080 bash "$repo_root/scripts/local/up_local_infra.sh")"

if ! grep -Fq 'Nacos Console: http://127.0.0.1:18080/index.html' <<<"$run_output"; then
  echo "Assertion failed: startup output must preserve the overridden Nacos Console URL." >&2
  exit 1
fi

if ! grep -Fq 'Nacos Client API: 127.0.0.1:8848' <<<"$run_output"; then
  echo "Assertion failed: startup output must preserve the Nacos Client API URL." >&2
  exit 1
fi

if [[ "$(grep -Fc 'curl:http://127.0.0.1:18080/v3/console/health/liveness' "$trace_file")" -lt 2 ]]; then
  echo "Assertion failed: startup must poll and confirm Nacos Console v3 liveness." >&2
  exit 1
fi

rendered_compose="$(docker compose -f "$repo_root/infrastructure/local/docker-compose.yml" config)"
if ! grep -Fq 'curl -f http://127.0.0.1:8080/v3/console/health/liveness' <<<"$rendered_compose"; then
  echo "Assertion failed: the Nacos container healthcheck must use Console port 8080 and v3 liveness." >&2
  exit 1
fi

echo "local infrastructure Nacos liveness checks passed"
