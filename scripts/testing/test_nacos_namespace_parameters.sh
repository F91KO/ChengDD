#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-nacos-namespace.XXXXXX)"
fixture_bin="$fixture_root/bin"
requests_file="$fixture_root/requests.jsonl"
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
import sys

args = sys.argv[1:]
url = args[-1]
fields = []
i = 0
while i < len(args) - 1:
    if args[i] == "--data-urlencode":
        fields.append(args[i + 1])
        i += 2
    else:
        i += 1

with open(os.environ["CDD_TEST_REQUESTS_FILE"], "a", encoding="utf-8") as log:
    log.write(json.dumps({"url": url, "fields": fields}) + "\n")

if url.endswith("/nacos/v1/cs/configs"):
    print("shared", end="")
elif url.endswith("/nacos/v1/ns/instance/list"):
    print('{"hosts":[]}', end="")
else:
    raise SystemExit(91)
PY
chmod +x "$fixture_bin/curl"

run_checker() {
  local namespace="$1"
  : >"$requests_file"
  env -u CDD_NACOS_USERNAME -u CDD_NACOS_PASSWORD \
    PATH="$fixture_bin:$PATH" \
    CDD_TEST_REQUESTS_FILE="$requests_file" \
    CDD_NACOS_NAMESPACE="$namespace" \
    CDD_NACOS_GROUP=CHENGDD_EXACT \
    CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1 \
    CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2 \
    bash "$repo_root/scripts/nacos/check_nacos_state.sh" qa stopped >/dev/null
}

assert_requests() {
  local expected_namespace="$1"
  python3 - "$requests_file" "$expected_namespace" <<'PY'
import json
import pathlib
import sys

requests = [json.loads(line) for line in pathlib.Path(sys.argv[1]).read_text().splitlines()]
namespace = sys.argv[2]
services = [
    "cdd-gateway",
    "cdd-auth-service",
    "cdd-merchant-service",
    "cdd-decoration-service",
    "cdd-product-service",
    "cdd-order-service",
    "cdd-marketing-service",
    "cdd-release-service",
    "cdd-report-service",
    "cdd-config-service",
]

assert len(requests) == 11, requests
config = requests[0]
expected_config = ["dataId=cdd-common-qa.yaml", "group=CHENGDD_EXACT"]
if namespace:
    expected_config.append(f"tenant={namespace}")
assert config["url"].endswith("/nacos/v1/cs/configs"), config
assert config["fields"] == expected_config, config

for request, service in zip(requests[1:], services):
    expected = [f"serviceName={service}", "groupName=CHENGDD_EXACT"]
    if namespace:
        expected.append(f"namespaceId={namespace}")
    assert request["url"].endswith("/nacos/v1/ns/instance/list"), request
    assert request["fields"] == expected, (service, request)
PY
}

run_checker "team-a/special namespace"
assert_requests "team-a/special namespace"

run_checker ""
assert_requests ""

echo "nacos namespace parameter behavior checks passed"
