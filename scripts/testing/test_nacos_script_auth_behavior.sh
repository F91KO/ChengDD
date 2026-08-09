#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-nacos-auth.XXXXXX)"
fixture_bin="$fixture_root/bin"
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
  '[[ "$request" == *"--connect-timeout"* && "$request" == *"--max-time"* ]] || exit 90' \
  'if [[ "$request" == *"/nacos/v3/auth/user/login"* ]]; then' \
  '  [[ "$request" == *"username=fixture-user"* && "$request" == *"password=fixture-password"* ]] || exit 91' \
  '  printf "%s" "{\"accessToken\":\"fixture-token\"}"' \
  '  exit 0' \
  'fi' \
  'if [[ "${CDD_TEST_REQUIRE_AUTH:-0}" == "1" ]]; then [[ "$request" == *"Authorization: Bearer fixture-token"* ]] || exit 92; fi' \
  'if [[ "$request" == *"/nacos/v1/cs/configs"* ]]; then' \
  '  if [[ "$request" == *"--request POST"* ]]; then printf true; else printf shared; fi' \
  'else' \
  '  printf "{\"hosts\":[]}"' \
  'fi' >"$fixture_bin/curl"
chmod +x "$fixture_bin/curl"

run_with_credentials() {
  env PATH="$fixture_bin:$PATH" \
    CDD_NACOS_USERNAME=fixture-user \
    CDD_NACOS_PASSWORD=fixture-password \
    CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1 \
    CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2 \
    "$@"
}

run_without_credentials() {
  env -u CDD_NACOS_USERNAME -u CDD_NACOS_PASSWORD \
    PATH="$fixture_bin:$PATH" \
    CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1 \
    CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2 \
    "$@"
}

if ! CDD_TEST_REQUIRE_AUTH=1 run_with_credentials bash "$repo_root/scripts/nacos/check_nacos_state.sh" local stopped >/dev/null; then
  echo "Assertion failed: Nacos state checker must authenticate and bound requests." >&2
  exit 1
fi
if ! CDD_TEST_REQUIRE_AUTH=1 run_with_credentials bash "$repo_root/scripts/nacos/publish_nacos_configs.sh" local >/dev/null; then
  echo "Assertion failed: Nacos publisher must authenticate and bound requests." >&2
  exit 1
fi
run_without_credentials bash "$repo_root/scripts/nacos/check_nacos_state.sh" local stopped >/dev/null

echo "nacos script authentication behavior checks passed"
