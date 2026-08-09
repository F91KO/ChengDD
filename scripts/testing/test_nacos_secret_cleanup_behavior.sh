#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-nacos-cleanup.XXXXXX)"
fixture_bin="$fixture_root/bin"
fixture_tmp="$fixture_root/tmp"
state_root="$fixture_root/state"
mkdir -p "$fixture_bin" "$fixture_tmp" "$state_root"

cleanup() {
  local status=$?
  /bin/rm -rf "$fixture_root"
  exit "$status"
}
trap cleanup EXIT

cat >"$fixture_bin/curl" <<'PY'
#!/usr/bin/env python3
import json
import os
import pathlib
import sys

args = sys.argv[1:]
url = args[-1]

if url.endswith("/nacos/v3/auth/user/login"):
    secret_paths = []
    i = 0
    while i < len(args) - 1:
        if args[i] == "--data-urlencode" and "@" in args[i + 1]:
            secret_paths.append(args[i + 1].split("@", 1)[1])
            i += 2
        else:
            i += 1
    pathlib.Path(os.environ["CDD_TEST_SECRET_PATHS"]).write_text(
        "\n".join(secret_paths), encoding="utf-8"
    )

    print(json.dumps({"accessToken": "fixture-token"}), end="")
    raise SystemExit(0)

if url.endswith("/nacos/v1/cs/configs"):
    print("true" if "--request" in args else "shared", end="")
elif url.endswith("/nacos/v1/ns/instance/list"):
    print('{"hosts":[]}', end="")
else:
    raise SystemExit(98)
PY

cat >"$fixture_bin/chmod" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
if [[ "${CDD_TEST_MODE:-}" == signal-* && "$*" == *"/username"* ]]; then
  printf '%s\n' "$1" "$2" >"${CDD_TEST_SECRET_PATHS:?}"
  signal_name="${CDD_TEST_MODE#signal-}"
  kill "-$signal_name" "$PPID"
  sleep 0.1
  exit 99
fi
if [[ "${CDD_TEST_MODE:-}" == "primary-42-cleanup-failure" && "$*" == *"/username"* ]]; then
  exit 42
fi
exec /bin/chmod "$@"
SH

cat >"$fixture_bin/rm" <<'SH'
#!/usr/bin/env bash
set -u
/bin/rm "$@"
rm_status=$?
if [[ "${CDD_TEST_MODE:-}" == *"cleanup-failure" ]]; then
  exit 1
fi
exit "$rm_status"
SH

/bin/chmod +x "$fixture_bin/curl" "$fixture_bin/chmod" "$fixture_bin/rm"

run_script() {
  local mode="$1"
  shift
  (
    export PATH="$fixture_bin:$PATH"
    export TMPDIR="$fixture_tmp"
    export CDD_TEST_MODE="$mode"
    export CDD_TEST_SECRET_PATHS="$state_root/secret-paths"
    export CDD_NACOS_USERNAME=fixture-user
    export CDD_NACOS_PASSWORD=fixture-password
    export CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1
    export CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2
    "$@"
  )
}

assert_secret_files_removed() {
  local label="$1"
  if [[ -f "$state_root/secret-paths" ]]; then
    while IFS= read -r secret_path; do
      if [[ -n "$secret_path" && -e "$secret_path" ]]; then
        echo "Assertion failed: temporary credential remained after $label." >&2
        exit 1
      fi
    done <"$state_root/secret-paths"
  fi
  if find "$fixture_tmp" -mindepth 1 -print -quit | grep -q .; then
    echo "Assertion failed: temporary auth directory remained after $label." >&2
    exit 1
  fi
}

assert_exit_status() {
  local label="$1"
  local expected="$2"
  local mode="$3"
  shift 3
  : >"$state_root/secret-paths"
  set +e
  run_script "$mode" "$@" >"$state_root/output" 2>&1
  actual=$?
  set -e
  if [[ "$actual" -ne "$expected" ]]; then
    echo "Assertion failed: $label exited $actual; expected $expected." >&2
    exit 1
  fi
  assert_secret_files_removed "$label"
}

scripts=(
  "publisher:bash:$repo_root/scripts/nacos/publish_nacos_configs.sh:local"
  "checker:bash:$repo_root/scripts/nacos/check_nacos_state.sh:local:stopped"
)

for script_spec in "${scripts[@]}"; do
  IFS=: read -r label command script arg1 arg2 <<<"$script_spec"
  command_args=("$command" "$script" "$arg1")
  [[ -n "${arg2:-}" ]] && command_args+=("$arg2")
  assert_exit_status "$label HUP" 129 signal-HUP "${command_args[@]}"
  assert_exit_status "$label INT" 130 signal-INT "${command_args[@]}"
  assert_exit_status "$label TERM" 143 signal-TERM "${command_args[@]}"
  assert_exit_status "$label primary failure" 42 primary-42-cleanup-failure "${command_args[@]}"
  assert_exit_status "$label cleanup failure" 1 cleanup-failure "${command_args[@]}"
done

echo "nacos secret cleanup behavior checks passed"
