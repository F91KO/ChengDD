#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-startup-safety.XXXXXX)"
fixture_repo="$fixture_root/repo"
fixture_parent="$fixture_root/parent"
fixture_bin="$fixture_root/bin"
fixture_java_home="$fixture_root/java-home"
fixture_state_dir="$fixture_root/runtime-state"
java_pid_file="$fixture_root/java.pid"
java_trace_file="$fixture_root/java.trace"
curl_trace_file="$fixture_root/curl.trace"
mkdir -p "$fixture_repo" "$fixture_parent/cdd-gateway/target" "$fixture_bin" "$fixture_java_home/bin" "$fixture_state_dir"
touch "$fixture_parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar" "$fixture_root/settings.xml"

cleanup() {
  local code=$?
  if [[ -s "$java_pid_file" ]]; then
    fixture_pid="$(<"$java_pid_file")"
    kill -KILL "$fixture_pid" >/dev/null 2>&1 || true
  fi
  jobs -pr | xargs -r kill -KILL >/dev/null 2>&1 || true
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

cat >"$fixture_bin/inspect-port" <<'EOF'
#!/usr/bin/env bash
case "$CDD_TEST_PORT_MODE" in
  occupied) printf '%s\n' "$CDD_TEST_UNRELATED_PID" ;;
  racing) [[ ! -s "$CDD_TEST_JAVA_PID_FILE" ]] || printf '%s\n' "$CDD_TEST_UNRELATED_PID" ;;
  owned) [[ ! -s "$CDD_TEST_JAVA_PID_FILE" ]] || cat "$CDD_TEST_JAVA_PID_FILE" ;;
  clear|unhealthy) ;;
  *) exit 1 ;;
esac
EOF
chmod +x "$fixture_bin/inspect-port"

cat >"$fixture_bin/mvn" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$fixture_bin/mvn"

cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
request="$*"
pid=""
previous=""
for argument in "$@"; do
  if [[ "$previous" == "-p" ]]; then pid="$argument"; fi
  previous="$argument"
done
if [[ "$request" == *"lstart="* ]]; then printf 'fixture-marker-%s\n' "$pid"; exit 0; fi
if [[ "$request" == *"command="* ]]; then printf 'java -jar cdd-gateway-0.1.0-SNAPSHOT.jar --server.port=8080\n'; exit 0; fi
exit 1
EOF
chmod +x "$fixture_bin/ps"

cat >"$fixture_java_home/bin/java" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$$" >"$CDD_TEST_JAVA_PID_FILE"
printf 'java %s\n' "$*" >>"$CDD_TEST_JAVA_TRACE"
if [[ "$CDD_TEST_PORT_MODE" == "unhealthy" ]]; then
  trap '' TERM
  while :; do :; done
fi
started_at="$(date +%s)"
while (( $(date +%s) - started_at < 3 )); do :; done
EOF
chmod +x "$fixture_java_home/bin/java"

cat >"$fixture_bin/curl" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >>"$CDD_TEST_CURL_TRACE"
if [[ "$CDD_TEST_PORT_MODE" != "unhealthy" ]]; then
  exit 0
fi
max_time=""
previous=""
for argument in "$@"; do
  if [[ "$previous" == "--max-time" ]]; then max_time="$argument"; fi
  previous="$argument"
done
if [[ -z "$max_time" ]]; then
  sleep 30
else
  sleep "$max_time"
fi
exit 1
EOF
chmod +x "$fixture_bin/curl"

run_module() {
  local mode="$1"
  rm -f "$java_pid_file" "$java_trace_file" "$curl_trace_file"
  rm -rf "$fixture_state_dir"
  mkdir -p "$fixture_state_dir"
  env \
    PATH="$fixture_bin:$PATH" \
    JAVA_HOME="$fixture_java_home" \
    CDD_ENV=local \
    CDD_CONFIG_MODE=file \
    CDD_RUNTIME_STATE_DIR="$fixture_state_dir" \
    CDD_RUNTIME_PORT_INSPECTOR="$fixture_bin/inspect-port" \
    CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS=5 \
    CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS=2 \
    CDD_TEST_PORT_MODE="$mode" \
    CDD_TEST_UNRELATED_PID="$$" \
    CDD_TEST_JAVA_PID_FILE="$java_pid_file" \
    CDD_TEST_JAVA_TRACE="$java_trace_file" \
    CDD_TEST_CURL_TRACE="$curl_trace_file" \
    bash -c 'source "$1"; run_packaged_module "$2" "$3" "$4" "$5" cdd-gateway gateway 8080' _ \
      "$repo_root/scripts/local/run_packaged_module.sh" "$fixture_repo" "$fixture_parent" "$fixture_root/settings.xml" "$fixture_root/m2"
}

if run_module occupied; then
  echo "Assertion failed: an occupied target port must be rejected." >&2
  exit 1
fi
[[ ! -e "$java_trace_file" ]] || {
  echo "Assertion failed: Java was invoked for an already occupied port." >&2
  exit 1
}
[[ ! -e "$fixture_state_dir/gateway.env" ]] || {
  echo "Assertion failed: occupied-port startup published runtime state." >&2
  exit 1
}

if run_module racing; then
  echo "Assertion failed: a racing healthy responder must not be adopted." >&2
  exit 1
fi
rg -F -- '/actuator/health' "$curl_trace_file" >/dev/null || {
  echo "Assertion failed: racing responder did not reach the post-health ownership check." >&2
  exit 1
}
[[ ! -e "$fixture_state_dir/gateway.env" ]] || {
  echo "Assertion failed: racing responder startup published runtime state." >&2
  exit 1
}
if [[ -s "$java_pid_file" ]] && kill -0 "$(<"$java_pid_file")" >/dev/null 2>&1; then
  echo "Assertion failed: racing responder left the launched process alive." >&2
  exit 1
fi

started_at="$(date +%s)"
run_module unhealthy &
runner_pid=$!
runner_status=0
deadline=$(( started_at + 6 ))
while kill -0 "$runner_pid" >/dev/null 2>&1 && (( $(date +%s) < deadline )); do
  sleep 0.1
done
if kill -0 "$runner_pid" >/dev/null 2>&1; then
  kill -KILL "$runner_pid" >/dev/null 2>&1 || true
  wait "$runner_pid" >/dev/null 2>&1 || true
  if [[ -s "$java_pid_file" ]]; then
    kill -KILL "$(<"$java_pid_file")" >/dev/null 2>&1 || true
  fi
  echo "Assertion failed: unhealthy startup exceeded its wall-clock deadline." >&2
  exit 1
fi
wait "$runner_pid" || runner_status=$?
[[ "$runner_status" -ne 0 ]] || {
  echo "Assertion failed: unhealthy startup returned success." >&2
  exit 1
}
elapsed_seconds=$(( $(date +%s) - started_at ))
(( elapsed_seconds <= 5 )) || {
  echo "Assertion failed: unhealthy startup cleanup was not bounded (${elapsed_seconds}s)." >&2
  exit 1
}
rg -F -- '--connect-timeout' "$curl_trace_file" >/dev/null || {
  echo "Assertion failed: health curl had no connect timeout." >&2
  exit 1
}
rg -F -- '--max-time' "$curl_trace_file" >/dev/null || {
  echo "Assertion failed: health curl had no overall timeout." >&2
  exit 1
}
if [[ -s "$java_pid_file" ]] && kill -0 "$(<"$java_pid_file")" >/dev/null 2>&1; then
  echo "Assertion failed: unhealthy startup left an owned process alive." >&2
  exit 1
fi
[[ ! -e "$fixture_state_dir/gateway.env" ]] || {
  echo "Assertion failed: unhealthy startup published runtime state." >&2
  exit 1
}

run_module owned
[[ -f "$fixture_state_dir/gateway.env" ]] || {
  echo "Assertion failed: an owned healthy listener did not publish runtime state." >&2
  exit 1
}
if find "$fixture_state_dir" -name '*.tmp.*' -print -quit | grep -q .; then
  echo "Assertion failed: atomic runtime state publication left a temporary file." >&2
  exit 1
fi

echo "runtime startup safety checks passed"
