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

cat >"$fixture_bin/curl" <<'PY'
#!/usr/bin/env python3
import pathlib
import sys

args = sys.argv[1:]
if "--connect-timeout" not in args or "--max-time" not in args:
    raise SystemExit(90)

def value_from_at_argument(raw):
    name, separator, path = raw.partition("@")
    if not separator:
        name, _, value = raw.partition("=")
        return name, value
    return name, pathlib.Path(path).read_text(encoding="utf-8")

fields = {}
headers = []
i = 0
while i < len(args) - 1:
    if args[i] == "--data-urlencode":
        name, value = value_from_at_argument(args[i + 1])
        fields[name] = value
        i += 2
    elif args[i] in {"--header", "-H"}:
        header = args[i + 1]
        if header.startswith("@"):
            header = pathlib.Path(header[1:]).read_text(encoding="utf-8")
        headers.append(header)
        i += 2
    else:
        i += 1

url = args[-1]
if url.endswith("/nacos/v3/auth/user/login"):
    if fields.get("username") != "fixture-user" or fields.get("password") != "fixture-password":
        raise SystemExit(91)
    print('{"accessToken":"fixture-token"}', end="")
    raise SystemExit(0)
if __import__("os").environ.get("CDD_TEST_REQUIRE_AUTH", "0") == "1":
    if "Authorization: Bearer fixture-token" not in headers:
        raise SystemExit(92)
if url.endswith("/nacos/v1/cs/configs"):
    print("true" if "--request" in args else "shared", end="")
else:
    print('{"hosts":[]}', end="")
PY
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
