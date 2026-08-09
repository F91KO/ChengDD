#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-nacos-isolation.XXXXXX)"
fixture_bin="$fixture_root/bin"
mkdir -p "$fixture_bin"

cleanup() {
  local status=$?
  rm -rf "$fixture_root"
  exit "$status"
}
trap cleanup EXIT

cat >"$fixture_bin/curl" <<'PY'
#!/usr/bin/env python3
import json
import os
import pathlib
import shutil
import sys
import urllib.parse

args = sys.argv[1:]
state = pathlib.Path(os.environ["CDD_TEST_NACOS_STATE"])
mode = os.environ.get("CDD_TEST_NACOS_MODE", "success")
require_auth = os.environ.get("CDD_TEST_NACOS_REQUIRE_AUTH", "0") == "1"
url = args[-1]
parsed = urllib.parse.urlparse(url)
method = "GET"
fields = {}
headers = []

i = 0
while i < len(args) - 1:
    arg = args[i]
    if arg == "--request":
        method = args[i + 1]
        i += 2
    elif arg == "--data-urlencode":
        key, _, value = args[i + 1].partition("=")
        fields[key] = value
        i += 2
    elif arg == "-H":
        headers.append(args[i + 1])
        i += 2
    else:
        i += 1

query = urllib.parse.parse_qs(parsed.query)
for key, values in query.items():
    fields.setdefault(key, values[-1])

with (state / "requests.log").open("a", encoding="utf-8") as log:
    log.write(json.dumps({"method": method, "url": url, "fields": fields}) + "\n")

if parsed.path.endswith("/v3/console/health/liveness"):
    if parsed.netloc != "127.0.0.1:19080":
        raise SystemExit(98)
    print("UP", end="")
    raise SystemExit(0)

if parsed.path.endswith("/nacos/v3/auth/user/login"):
    if parsed.netloc != "127.0.0.1:19848":
        raise SystemExit(98)
    if fields.get("username") != "fixture-user" or fields.get("password") != "fixture-password":
        raise SystemExit(91)
    print('{"accessToken":"fixture-token"}', end="")
    raise SystemExit(0)

if parsed.path.startswith("/v3/console/") and parsed.netloc != "127.0.0.1:19080":
    raise SystemExit(98)
if parsed.path.startswith("/nacos/") and parsed.netloc != "127.0.0.1:19848":
    raise SystemExit(98)
if require_auth and "Authorization: Bearer fixture-token" not in headers:
    raise SystemExit(92)

if parsed.path.endswith("/v3/console/core/namespace/exist"):
    namespace = fields.get("customNamespaceId", "")
    exists = (state / "namespaces" / namespace).is_dir()
    print(json.dumps({"code": 0, "message": "success", "data": exists}), end="")
    raise SystemExit(0)

if parsed.path.endswith("/v3/console/core/namespace"):
    namespace = fields.get("customNamespaceId") or fields.get("namespaceId", "")
    namespace_dir = state / "namespaces" / namespace
    if method == "POST":
        if not namespace or namespace in {"public", "shared-live"} or namespace_dir.exists():
            raise SystemExit(93)
        namespace_dir.mkdir(parents=True)
        print('{"code":0,"message":"success","data":true}', end="")
        raise SystemExit(0)
    if method == "DELETE":
        if mode == "cleanup-hard-failure":
            raise SystemExit(28)
        shutil.rmtree(namespace_dir, ignore_errors=True)
        if mode == "cleanup-ambiguous":
            marker = state / "cleanup-ambiguous-fired"
            if not marker.exists():
                marker.touch()
                raise SystemExit(28)
        print('{"code":0,"message":"success","data":true}', end="")
        raise SystemExit(0)

if parsed.path.endswith("/nacos/v1/cs/configs"):
    namespace = fields.get("tenant", "")
    namespace_dir = state / "namespaces" / namespace
    if namespace in {"", "public", "shared-live"} or not namespace_dir.is_dir():
        (state / "active-namespace-mutation").write_text(namespace, encoding="utf-8")
        raise SystemExit(94)
    if method == "POST":
        (namespace_dir / fields["dataId"]).write_text(fields.get("content", ""), encoding="utf-8")
        if mode == "publish-ambiguous" and fields.get("dataId") == "cdd-nacos-contract-test-local.yaml":
            raise SystemExit(28)
        print("true", end="")
        raise SystemExit(0)
    raise SystemExit(95)

if parsed.path.endswith("/nacos/v1/ns/instance/list"):
    namespace = fields.get("namespaceId", "")
    if namespace in {"", "public", "shared-live"} or not (state / "namespaces" / namespace).is_dir():
        raise SystemExit(96)
    (state / "queried-namespace").write_text(namespace, encoding="utf-8")
    print('{"hosts":[]}', end="")
    raise SystemExit(0)

raise SystemExit(97)
PY
chmod +x "$fixture_bin/curl"

cat >"$fixture_bin/docker" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
[[ "$*" == *"compose"* && "$*" == *"up -d nacos"* ]]
SH
chmod +x "$fixture_bin/docker"

cat >"$fixture_bin/mvn" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
state="${CDD_TEST_NACOS_STATE:?}"
namespace="${CDD_NACOS_NAMESPACE:?}"
[[ "$namespace" == "${CDD_NACOS_TEST_NAMESPACE_ID:?}" ]]
[[ -d "$state/namespaces/$namespace" ]]
[[ "${CDD_NACOS_SERVER_ADDR:?}" == "127.0.0.1:19848" ]]
[[ "${CDD_NACOS_GROUP:?}" == "CHENGDD_TEST" ]]
printf '%s' "$namespace" >"$state/maven-namespace"
SH
chmod +x "$fixture_bin/mvn"

assert_active_configs_unchanged() {
  local state="$1"
  cmp "$state/before/cdd-common-local.yaml" "$state/active/cdd-common-local.yaml"
  cmp "$state/before/cdd-nacos-contract-test-local.yaml" "$state/active/cdd-nacos-contract-test-local.yaml"
  cmp "$state/before/unrelated.yaml" "$state/active/unrelated.yaml"
  [[ ! -e "$state/active-namespace-mutation" ]]
}

prepare_state() {
  local scenario="$1"
  local state="$fixture_root/$scenario"
  mkdir -p "$state/active" "$state/before" "$state/namespaces"
  printf 'shared: original\ntrailing-space: value \n' >"$state/active/cdd-common-local.yaml"
  printf 'service: original\n' >"$state/active/cdd-nacos-contract-test-local.yaml"
  printf 'unrelated: 保持不变\n' >"$state/active/unrelated.yaml"
  cp "$state/active/cdd-common-local.yaml" "$state/before/cdd-common-local.yaml"
  cp "$state/active/cdd-nacos-contract-test-local.yaml" "$state/before/cdd-nacos-contract-test-local.yaml"
  cp "$state/active/unrelated.yaml" "$state/before/unrelated.yaml"
  printf '%s' "$state"
}

run_harness() {
  local state="$1"
  local mode="$2"
  local namespace="$3"
  local auth_mode="${4:-auth}"
  local common_env=(
    PATH="$fixture_bin:$PATH"
    CDD_TEST_NACOS_STATE="$state"
    CDD_TEST_NACOS_MODE="$mode"
    CDD_NACOS_SERVER_ADDR=127.0.0.1:19848
    CDD_NACOS_CONSOLE_ADDR=127.0.0.1:19080
    CDD_NACOS_GROUP=CHENGDD_TEST
    CDD_NACOS_NAMESPACE=shared-live
    CDD_NACOS_TEST_NAMESPACE_ID="$namespace"
    CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1
    CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2
  )
  if [[ "$auth_mode" == "auth" ]]; then
    env "${common_env[@]}" \
      CDD_TEST_NACOS_REQUIRE_AUTH=1 \
      CDD_NACOS_USERNAME=fixture-user \
      CDD_NACOS_PASSWORD=fixture-password \
      bash "$repo_root/scripts/testing/run_nacos_integration.sh"
  else
    env -u CDD_NACOS_USERNAME -u CDD_NACOS_PASSWORD \
      "${common_env[@]}" \
      CDD_TEST_NACOS_REQUIRE_AUTH=0 \
      bash "$repo_root/scripts/testing/run_nacos_integration.sh"
  fi
}

success_state="$(prepare_state success)"
success_namespace="cdd-nacos-it-success-001"
run_harness "$success_state" cleanup-ambiguous "$success_namespace" >/dev/null
assert_active_configs_unchanged "$success_state"
[[ ! -d "$success_state/namespaces/$success_namespace" ]]
[[ "$(cat "$success_state/maven-namespace")" == "$success_namespace" ]]
[[ "$(cat "$success_state/queried-namespace")" == "$success_namespace" ]]

failure_state="$(prepare_state ambiguous-publish)"
failure_namespace="cdd-nacos-it-publish-failure-001"
if run_harness "$failure_state" publish-ambiguous "$failure_namespace" no-auth >/dev/null 2>&1; then
  echo "Assertion failed: ambiguous publish failure must remain visible." >&2
  exit 1
fi
assert_active_configs_unchanged "$failure_state"
[[ ! -d "$failure_state/namespaces/$failure_namespace" ]]

cleanup_failure_state="$(prepare_state cleanup-hard-failure)"
cleanup_failure_namespace="cdd-nacos-it-cleanup-failure-001"
if run_harness "$cleanup_failure_state" cleanup-hard-failure "$cleanup_failure_namespace" >/dev/null 2>&1; then
  echo "Assertion failed: namespace cleanup failure must fail the harness." >&2
  exit 1
fi
assert_active_configs_unchanged "$cleanup_failure_state"
[[ -d "$cleanup_failure_state/namespaces/$cleanup_failure_namespace" ]]

echo "nacos integration namespace isolation checks passed"
