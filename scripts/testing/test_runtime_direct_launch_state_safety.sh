#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-direct-state.XXXXXX)"
fixture_parent="$fixture_root/parent"
fixture_repo="$fixture_root/repo"
fixture_bin="$fixture_root/bin"
fixture_java_home="$fixture_root/java-home"
state_dir="$fixture_root/runtime-state"
trace_file="$fixture_root/action.trace"
state_before="$fixture_root/gateway.before"
mkdir -p "$fixture_parent/cdd-gateway/target" "$fixture_repo" "$fixture_bin" "$fixture_java_home/bin" "$state_dir"
jar_path="$fixture_parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar"
touch "$jar_path" "$fixture_root/settings.xml"

cleanup() {
  local code=$?
  kill -KILL "${old_pid:-}" >/dev/null 2>&1 || true
  wait "${old_pid:-}" >/dev/null 2>&1 || true
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

cat >"$fixture_bin/mvn" <<'EOF'
#!/usr/bin/env bash
printf 'mvn\n' >>"$CDD_TEST_ACTION_TRACE"
exit 0
EOF
cat >"$fixture_bin/inspect-port" <<'EOF'
#!/usr/bin/env bash
printf 'inspect-port\n' >>"$CDD_TEST_ACTION_TRACE"
exit 0
EOF
cat >"$fixture_java_home/bin/java" <<'EOF'
#!/usr/bin/env bash
printf 'java\n' >>"$CDD_TEST_ACTION_TRACE"
exit 1
EOF
cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
request="$*"
if [[ "$request" == *"lstart="* ]]; then printf '%s\n' "$CDD_TEST_OLD_MARKER"; exit 0; fi
if [[ "$request" == *"command="* ]]; then printf '/bin/bash -jar %s --server.port=19080\n' "$CDD_TEST_JAR_PATH"; exit 0; fi
exit 1
EOF
chmod +x "$fixture_bin"/* "$fixture_java_home/bin/java"

/bin/bash -c 'trap "" TERM; while :; do sleep 1; done' -jar "$jar_path" --server.port=19080 &
old_pid=$!
source "$repo_root/scripts/local/backend_runtime_guard.sh"
old_marker="$(PATH="$fixture_bin:$PATH" CDD_TEST_OLD_MARKER="marker-${old_pid}" backend_runtime_process_start_marker "$old_pid")"
cat >"$state_dir/gateway.env" <<EOF
SERVICE_NAME=gateway
MODULE_NAME=cdd-gateway
SERVICE_PORT=19080
SERVICE_PID=${old_pid}
PROCESS_START_MARKER=${old_marker}
JAVA_PATH=/bin/bash
JAR_PATH=${jar_path}
GIT_HEAD=fixture
BACKEND_FINGERPRINT=fixture
STARTED_AT=0
STARTED_AT_TEXT=fixture
EOF
cp "$state_dir/gateway.env" "$state_before"

if PATH="$fixture_bin:$PATH" \
  JAVA_HOME="$fixture_java_home" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_PORT_INSPECTOR="$fixture_bin/inspect-port" \
  CDD_TEST_ACTION_TRACE="$trace_file" \
  CDD_TEST_OLD_MARKER="$old_marker" \
  CDD_TEST_JAR_PATH="$jar_path" \
  bash -c 'source "$1"; run_packaged_module "$2" "$3" "$4" "$5" cdd-gateway gateway' _ \
    "$repo_root/scripts/local/run_packaged_module.sh" "$fixture_repo" "$fixture_parent" "$fixture_root/settings.xml" "$fixture_root/m2"; then
  echo "Assertion failed: direct launch replaced a live old-override service." >&2
  exit 1
fi

cmp -s "$state_before" "$state_dir/gateway.env" || {
  echo "Assertion failed: direct launch changed existing live service state." >&2
  exit 1
}
if [[ -s "$trace_file" ]]; then
  echo "Assertion failed: direct launch performed build/port/Java action before rejecting live state." >&2
  exit 1
fi
kill -0 "$old_pid" >/dev/null 2>&1 || {
  echo "Assertion failed: direct launch killed the existing owned service." >&2
  exit 1
}

echo "runtime direct-launch existing-state safety checks passed"
