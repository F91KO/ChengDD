#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-stop-evolving.XXXXXX)"
fixture_bin="$fixture_root/bin"
state_dir="$fixture_root/runtime-state"
launcher_script="$fixture_root/run_gateway.sh"
root_pid_file="$fixture_root/root.pid"
child_pid_file="$fixture_root/child.pid"
late_pid_file="$fixture_root/late.pid"
term_trace="$fixture_root/term.trace"
pgrep_pid_file="$fixture_root/pgrep.pid"
mkdir -p "$fixture_bin" "$state_dir/logs"

cleanup() {
  local code=$?
  for pid_file in "$root_pid_file" "$child_pid_file" "$late_pid_file" "$pgrep_pid_file"; do
    [[ -s "$pid_file" ]] || continue
    kill -KILL "$(<"$pid_file")" >/dev/null 2>&1 || true
  done
  kill -KILL "${unrelated_pid:-}" "${stop_runner_pid:-}" >/dev/null 2>&1 || true
  wait "${stop_runner_pid:-}" >/dev/null 2>&1 || true
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
request="$*"
pid=""
previous=""
for argument in "$@"; do if [[ "$previous" == "-p" ]]; then pid="$argument"; fi; previous="$argument"; done
if [[ "$request" == *"lstart="* ]]; then printf 'marker-%s\n' "$pid"; exit 0; fi
if [[ "$request" == *"state="* ]]; then printf 'T\n'; exit 0; fi
if [[ "$request" == *"command="* ]]; then printf 'bash %s\n' "$CDD_TEST_LAUNCHER_SCRIPT"; exit 0; fi
if [[ "$request" == *"-ax"* ]]; then exit 0; fi
exit 1
EOF
cat >"$fixture_bin/pgrep" <<'EOF'
#!/usr/bin/env bash
if [[ "${CDD_TEST_PGREP_MODE:-normal}" == "hang" ]]; then
  printf '%s\n' "$$" >"$CDD_TEST_PGREP_PID_FILE"
  trap '' TERM
  while :; do :; done
fi
if [[ "$2" == "$(<"$CDD_TEST_ROOT_PID_FILE")" ]]; then
  [[ ! -s "$CDD_TEST_CHILD_PID_FILE" ]] || cat "$CDD_TEST_CHILD_PID_FILE"
  [[ ! -s "$CDD_TEST_LATE_PID_FILE" ]] || cat "$CDD_TEST_LATE_PID_FILE"
  exit 0
fi
exit 1
EOF
chmod +x "$fixture_bin"/*

cat >"$launcher_script" <<'EOF'
#!/usr/bin/env bash
spawn_late_child() {
  printf 'root-term\n' >>"$CDD_TEST_TERM_TRACE"
  [[ -s "$CDD_TEST_LATE_PID_FILE" ]] && return
  bash -c 'trap "" TERM; while :; do :; done' &
  printf '%s\n' "$!" >"$CDD_TEST_LATE_PID_FILE"
}
trap spawn_late_child TERM
printf '%s\n' "$$" >"$CDD_TEST_ROOT_PID_FILE"
bash -c 'trap '\''printf "child-term\\n" >>"$1"'\'' TERM; while :; do :; done' _ "$CDD_TEST_TERM_TRACE" &
printf '%s\n' "$!" >"$CDD_TEST_CHILD_PID_FILE"
while :; do :; done
EOF
chmod +x "$launcher_script"

start_launcher_fixture() {
  rm -f "$root_pid_file" "$child_pid_file" "$late_pid_file" "$term_trace" "$pgrep_pid_file"
  CDD_TEST_ROOT_PID_FILE="$root_pid_file" CDD_TEST_CHILD_PID_FILE="$child_pid_file" CDD_TEST_LATE_PID_FILE="$late_pid_file" CDD_TEST_TERM_TRACE="$term_trace" bash "$launcher_script" &
  while [[ ! -s "$root_pid_file" || ! -s "$child_pid_file" ]]; do sleep 0.01; done
  root_pid="$(<"$root_pid_file")"
  cat >"$state_dir/logs/gateway.launcher.env" <<STATE
SERVICE_NAME=gateway
LAUNCHER_NAME=run_gateway.sh
LAUNCHER_PID=${root_pid}
PROCESS_START_MARKER=marker-${root_pid}
STATE
}

start_launcher_fixture
sleep 30 &
unrelated_pid=$!
if ! PATH="$fixture_bin:$PATH" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_STOP_TIMEOUT_SECONDS=6 \
  CDD_RUNTIME_TERM_GRACE_SECONDS=1 \
  CDD_TEST_LAUNCHER_SCRIPT="$launcher_script" \
  CDD_TEST_ROOT_PID_FILE="$root_pid_file" \
  CDD_TEST_CHILD_PID_FILE="$child_pid_file" \
  CDD_TEST_LATE_PID_FILE="$late_pid_file" \
  CDD_TEST_PGREP_PID_FILE="$pgrep_pid_file" \
  bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1; then
  echo "Assertion failed: global stop failed to terminate a proved evolving tree." >&2
  exit 1
fi
for pid_file in "$root_pid_file" "$child_pid_file" "$late_pid_file"; do
  [[ -s "$pid_file" ]] || { echo "Assertion failed: evolving stop fixture did not record a pid." >&2; exit 1; }
  if kill -0 "$(<"$pid_file")" >/dev/null 2>&1; then
    echo "Assertion failed: global stop left evolving-tree pid $(<"$pid_file") alive." >&2
    exit 1
  fi
done
kill -0 "$unrelated_pid" >/dev/null 2>&1 || { echo "Assertion failed: global stop killed unrelated pid." >&2; exit 1; }
[[ ! -e "$state_dir/logs/gateway.launcher.env" ]] || { echo "Assertion failed: stopped launcher state was not removed." >&2; exit 1; }

mkdir -p "$state_dir/logs"
start_launcher_fixture
cp "$state_dir/logs/gateway.launcher.env" "$fixture_root/launcher.before"
started_at="$(date +%s)"
PATH="$fixture_bin:$PATH" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_STOP_TIMEOUT_SECONDS=3 \
  CDD_RUNTIME_TERM_GRACE_SECONDS=1 \
  CDD_TEST_LAUNCHER_SCRIPT="$launcher_script" \
  CDD_TEST_ROOT_PID_FILE="$root_pid_file" \
  CDD_TEST_CHILD_PID_FILE="$child_pid_file" \
  CDD_TEST_LATE_PID_FILE="$late_pid_file" \
  CDD_TEST_PGREP_PID_FILE="$pgrep_pid_file" \
  CDD_TEST_PGREP_MODE=hang \
  bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1 &
stop_runner_pid=$!
watchdog_deadline=$(( started_at + 5 ))
while kill -0 "$stop_runner_pid" >/dev/null 2>&1 && (( $(date +%s) < watchdog_deadline )); do sleep 0.1; done
if kill -0 "$stop_runner_pid" >/dev/null 2>&1; then
  echo "Assertion failed: global stop descendant discovery exceeded its deadline." >&2
  exit 1
fi
stop_status=0
wait "$stop_runner_pid" || stop_status=$?
[[ "$stop_status" -ne 0 ]] || { echo "Assertion failed: unprovable global stop returned success." >&2; exit 1; }
for pid_file in "$root_pid_file" "$child_pid_file"; do
  kill -0 "$(<"$pid_file")" >/dev/null 2>&1 || { echo "Assertion failed: unprovable global stop killed pid $(<"$pid_file")." >&2; exit 1; }
done
[[ ! -s "$term_trace" ]] || { echo "Assertion failed: unprovable global stop signalled the launcher tree." >&2; exit 1; }
cmp -s "$fixture_root/launcher.before" "$state_dir/logs/gateway.launcher.env" || { echo "Assertion failed: unprovable global stop changed launcher state." >&2; exit 1; }
[[ -s "$pgrep_pid_file" ]] || { echo "Assertion failed: hung global pgrep fixture did not run." >&2; exit 1; }
if kill -0 "$(<"$pgrep_pid_file")" >/dev/null 2>&1; then
  echo "Assertion failed: global stop left hung pgrep alive." >&2
  exit 1
fi

echo "runtime stop evolving-tree checks passed"
