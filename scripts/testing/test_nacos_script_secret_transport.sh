#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-nacos-secret-transport.XXXXXX)"
fixture_bin="$fixture_root/bin"
fixture_tmp="$fixture_root/tmp"
state_root="$fixture_root/state"
mkdir -p "$fixture_bin" "$fixture_tmp" "$state_root"

cleanup() {
  local status=$?
  rm -rf "$fixture_root"
  exit "$status"
}
trap cleanup EXIT

username='fixture user+/?&%'
password='p@ss &=/+?%'
token='token value+/?&%'
config_marker='server: fixture-config-secret-marker'

cat >"$fixture_bin/curl" <<'PY'
#!/usr/bin/env python3
import json
import os
import pathlib
import stat
import sys

args = sys.argv[1:]
state = pathlib.Path(os.environ["CDD_TEST_STATE"])
secrets = [
    os.environ["CDD_TEST_USERNAME"],
    os.environ["CDD_TEST_PASSWORD"],
    os.environ["CDD_TEST_TOKEN"],
    os.environ["CDD_TEST_CONFIG_MARKER"],
]

with (state / "argv.jsonl").open("a", encoding="utf-8") as log:
    log.write(json.dumps(args) + "\n")

def read_at_file(value, require_private=True):
    if "@" not in value:
        return None, None
    name, path = value.split("@", 1)
    file_path = pathlib.Path(path)
    if not file_path.is_file():
        return None, None
    if require_private:
        mode = stat.S_IMODE(file_path.stat().st_mode)
        if mode != 0o600:
            raise SystemExit(93)
        with (state / "secret-files.jsonl").open("a", encoding="utf-8") as log:
            log.write(json.dumps(str(file_path)) + "\n")
    return name, file_path.read_text(encoding="utf-8")

fields = {}
headers = []
i = 0
while i < len(args) - 1:
    if args[i] == "--data-urlencode":
        raw = args[i + 1]
        name, value = read_at_file(raw, not raw.startswith("content@"))
        if name is None:
            name, _, value = raw.partition("=")
        fields[name] = value
        i += 2
    elif args[i] in {"--header", "-H"}:
        raw = args[i + 1]
        if raw.startswith("@"):
            _, value = read_at_file("header" + raw)
            headers.append(value)
        else:
            headers.append(raw)
        i += 2
    else:
        i += 1

url = args[-1]
if url.endswith("/nacos/v3/auth/user/login"):
    if fields.get("username") != os.environ["CDD_TEST_USERNAME"]:
        raise SystemExit(94)
    if fields.get("password") != os.environ["CDD_TEST_PASSWORD"]:
        raise SystemExit(95)
    if os.environ.get("CDD_TEST_MODE") == "reject-login":
        raise SystemExit(22)
    print(json.dumps({"accessToken": os.environ["CDD_TEST_TOKEN"]}), end="")
    raise SystemExit(0)

expected_header = "Authorization: Bearer " + os.environ["CDD_TEST_TOKEN"]
if expected_header not in headers:
    raise SystemExit(96)
if url.endswith("/nacos/v1/cs/configs"):
    if "--request" in args:
        if os.environ.get("CDD_TEST_MODE") == "reject-publish":
            print("false", end="")
        else:
            print("true", end="")
    else:
        print("shared", end="")
elif url.endswith("/nacos/v1/ns/instance/list"):
    print('{"hosts":[]}', end="")
else:
    raise SystemExit(97)
PY
chmod +x "$fixture_bin/curl"

run_script() {
  local mode="$1"
  shift
  (
    export PATH="$fixture_bin:$PATH"
    export TMPDIR="$fixture_tmp"
    export CDD_TEST_STATE="$state_root"
    export CDD_TEST_MODE="$mode"
    export CDD_TEST_USERNAME="$username"
    export CDD_TEST_PASSWORD="$password"
    export CDD_TEST_TOKEN="$token"
    export CDD_TEST_CONFIG_MARKER="$config_marker"
    export CDD_NACOS_USERNAME="$username"
    export CDD_NACOS_PASSWORD="$password"
    export CDD_NACOS_NAMESPACE='secure namespace+/?&%'
    export CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1
    export CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2
    "$@"
  )
}

assert_no_secret_argv_or_output() {
  local output_file="$1"
  CDD_TEST_USERNAME="$username" \
  CDD_TEST_PASSWORD="$password" \
  CDD_TEST_TOKEN="$token" \
  CDD_TEST_CONFIG_MARKER="$config_marker" \
    python3 - "$state_root/argv.jsonl" "$output_file" <<'PY'
import json
import os
import pathlib
import sys

argv = [item for line in pathlib.Path(sys.argv[1]).read_text().splitlines() for item in json.loads(line)]
output = pathlib.Path(sys.argv[2]).read_text(encoding="utf-8")
for secret in [os.environ[name] for name in ["CDD_TEST_USERNAME", "CDD_TEST_PASSWORD", "CDD_TEST_TOKEN", "CDD_TEST_CONFIG_MARKER"]]:
    assert all(secret not in item for item in argv), (secret, argv)
    assert secret not in output, (secret, output)
for item in argv:
    if item.startswith("content@"):
        content = pathlib.Path(item.split("@", 1)[1]).read_text(encoding="utf-8")
        assert all(content not in argument for argument in argv), item
        assert content not in output, item
PY
}

assert_temp_files_removed() {
  if find "$fixture_tmp" -mindepth 1 -print -quit | grep -q .; then
    echo "Assertion failed: Nacos scripts left temporary credential artifacts behind." >&2
    find "$fixture_tmp" -mindepth 1 -maxdepth 2 -print >&2
    exit 1
  fi
  if [[ -f "$state_root/secret-files.jsonl" ]]; then
    while IFS= read -r encoded_path; do
      path="$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read()))' <<<"$encoded_path")"
      [[ ! -e "$path" ]] || {
        echo "Assertion failed: temporary credential file still exists: $path" >&2
        exit 1
      }
    done <"$state_root/secret-files.jsonl"
  fi
}

: >"$state_root/argv.jsonl"
success_output="$state_root/success.output"
run_script success bash "$repo_root/scripts/nacos/publish_nacos_configs.sh" local >"$success_output" 2>&1
run_script success bash "$repo_root/scripts/nacos/check_nacos_state.sh" local stopped >>"$success_output" 2>&1
assert_no_secret_argv_or_output "$success_output"
assert_temp_files_removed

: >"$state_root/argv.jsonl"
: >"$state_root/secret-files.jsonl"
failure_output="$state_root/failure.output"
if run_script reject-publish bash "$repo_root/scripts/nacos/publish_nacos_configs.sh" local >"$failure_output" 2>&1; then
  echo "Assertion failed: rejected Nacos publish must remain a failure." >&2
  exit 1
fi
assert_no_secret_argv_or_output "$failure_output"
assert_temp_files_removed

: >"$state_root/argv.jsonl"
: >"$state_root/secret-files.jsonl"
login_failure_output="$state_root/login-failure.output"
if run_script reject-login bash "$repo_root/scripts/nacos/check_nacos_state.sh" local stopped >"$login_failure_output" 2>&1; then
  echo "Assertion failed: rejected Nacos login must remain a failure." >&2
  exit 1
fi
assert_no_secret_argv_or_output "$login_failure_output"
assert_temp_files_removed

echo "nacos script secret transport checks passed"
