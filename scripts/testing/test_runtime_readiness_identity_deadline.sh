#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-readiness-identity.XXXXXX)"
fixture_bin="$fixture_root/bin"
fixture_scripts="$fixture_root/scripts"
fixture_launchers="$fixture_root/launchers"
state_dir="$fixture_root/runtime-state"
launcher_pid_file="$fixture_root/launcher.pid"
child_pid_file="$fixture_root/child.pid"
identity_count_file="$fixture_root/identity.count"
hung_ps_pid_file="$fixture_root/hung-ps.pid"
mkdir -p "$fixture_bin" "$fixture_scripts" "$fixture_launchers" "$state_dir"

cleanup() {
  local code=$?
  for pid_file in "$launcher_pid_file" "$child_pid_file" "$hung_ps_pid_file"; do
    [[ -s "$pid_file" ]] || continue
    kill -KILL "$(<"$pid_file")" >/dev/null 2>&1 || true
  done
  kill -KILL "${runner_pid:-}" >/dev/null 2>&1 || true
  wait "${runner_pid:-}" >/dev/null 2>&1 || true
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

printf '%s\n' '#!/usr/bin/env bash' 'exit 1' >"$fixture_bin/lsof"
printf '%s\n' '#!/usr/bin/env bash' 'exit 1' >"$fixture_bin/curl"
cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
request="$*"
pid=""
previous=""
for argument in "$@"; do if [[ "$previous" == "-p" ]]; then pid="$argument"; fi; previous="$argument"; done
launcher_pid="$(<"$CDD_TEST_LAUNCHER_PID_FILE")"
child_pid="$(<"$CDD_TEST_CHILD_PID_FILE")"
if [[ "$request" == *"lstart="* ]]; then
  if [[ "$pid" == "$child_pid" ]]; then
    count=0
    [[ ! -s "$CDD_TEST_IDENTITY_COUNT_FILE" ]] || count="$(<"$CDD_TEST_IDENTITY_COUNT_FILE")"
    count=$((count + 1))
    printf '%s\n' "$count" >"$CDD_TEST_IDENTITY_COUNT_FILE"
    if [[ "$count" -eq 1 ]]; then
      printf '%s\n' "$$" >"$CDD_TEST_HUNG_PS_PID_FILE"
      trap '' TERM
      while :; do :; done
    fi
  fi
  printf 'marker-%s\n' "$pid"
  exit 0
fi
if [[ "$request" == *"state="* ]]; then printf 'T\n'; exit 0; fi
if [[ "$request" == *"command="* && "$pid" == "$launcher_pid" ]]; then printf 'bash %s/run_gateway.sh\n' "$CDD_RUNTIME_LAUNCHER_DIR"; exit 0; fi
if [[ "$request" == *"command="* && "$pid" == "$child_pid" ]]; then printf '/fixture/java -jar %s/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar --server.port=8080\n' "$CDD_TEST_REPO_ROOT"; exit 0; fi
exit 1
EOF
cat >"$fixture_bin/pgrep" <<'EOF'
#!/usr/bin/env bash
if [[ "$2" == "$(<"$CDD_TEST_LAUNCHER_PID_FILE")" && -s "$CDD_TEST_CHILD_PID_FILE" ]]; then cat "$CDD_TEST_CHILD_PID_FILE"; exit 0; fi
exit 1
EOF
for fixture_command in "$fixture_bin"/*; do chmod +x "$fixture_command"; done
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' >"$fixture_scripts/up.sh"
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' >"$fixture_scripts/migrate.sh"
chmod +x "$fixture_scripts"/*.sh

cat >"$fixture_launchers/run_gateway.sh" <<'EOF'
#!/usr/bin/env bash
trap '' TERM
printf '%s\n' "$$" >"$CDD_TEST_LAUNCHER_PID_FILE"
bash -c 'trap "" TERM; while :; do :; done' &
child_pid=$!
printf '%s\n' "$child_pid" >"$CDD_TEST_CHILD_PID_FILE"
mkdir -p "$CDD_RUNTIME_STATE_DIR"
cat >"$CDD_RUNTIME_STATE_DIR/gateway.env" <<STATE
SERVICE_NAME=gateway
MODULE_NAME=cdd-gateway
SERVICE_PORT=8080
SERVICE_PID=${child_pid}
PROCESS_START_MARKER=marker-${child_pid}
JAVA_PATH=/fixture/java
JAR_PATH=${CDD_TEST_REPO_ROOT}/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar
GIT_HEAD=fixture
BACKEND_FINGERPRINT=fixture
STARTED_AT=0
STARTED_AT_TEXT=fixture
STATE
while :; do :; done
EOF
chmod +x "$fixture_launchers/run_gateway.sh"
for launcher in run_auth_service_mysql.sh run_merchant_service_mysql.sh run_decoration_service_mysql.sh run_product_service_mysql.sh run_order_service_mysql.sh run_marketing_service_mysql.sh run_release_service_mysql.sh run_report_service_mysql.sh run_config_service_mysql.sh; do
  printf '%s\n' '#!/usr/bin/env bash' 'exit 99' >"$fixture_launchers/$launcher"
  chmod +x "$fixture_launchers/$launcher"
done

started_at="$(date +%s)"
PATH="$fixture_bin:$PATH" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_UP_INFRA_SCRIPT="$fixture_scripts/up.sh" \
  CDD_RUNTIME_MIGRATE_SCRIPT="$fixture_scripts/migrate.sh" \
  CDD_RUNTIME_LAUNCHER_DIR="$fixture_launchers" \
  CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS=8 \
  CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS=5 \
  CDD_TEST_LAUNCHER_PID_FILE="$launcher_pid_file" \
  CDD_TEST_CHILD_PID_FILE="$child_pid_file" \
  CDD_TEST_IDENTITY_COUNT_FILE="$identity_count_file" \
  CDD_TEST_HUNG_PS_PID_FILE="$hung_ps_pid_file" \
  CDD_TEST_REPO_ROOT="$repo_root" \
  bash "$repo_root/scripts/local/run_all_services_mysql.sh" >/dev/null 2>&1 &
runner_pid=$!
watchdog_deadline=$(( started_at + 10 ))
while kill -0 "$runner_pid" >/dev/null 2>&1 && (( $(date +%s) < watchdog_deadline )); do sleep 0.1; done
if kill -0 "$runner_pid" >/dev/null 2>&1; then
  echo "Assertion failed: hung readiness identity exceeded the run-all deadline." >&2
  exit 1
fi
runner_status=0
wait "$runner_pid" || runner_status=$?
[[ "$runner_status" -ne 0 ]] || { echo "Assertion failed: hung readiness identity returned success." >&2; exit 1; }
elapsed_seconds=$(( $(date +%s) - started_at ))
(( elapsed_seconds <= 9 )) || { echo "Assertion failed: identity cleanup was not bounded (${elapsed_seconds}s)." >&2; exit 1; }
for pid_file in "$launcher_pid_file" "$child_pid_file" "$hung_ps_pid_file"; do
  [[ -s "$pid_file" ]] || { echo "Assertion failed: fixture did not record ${pid_file}." >&2; exit 1; }
  if kill -0 "$(<"$pid_file")" >/dev/null 2>&1; then
    echo "Assertion failed: readiness identity failure left pid $(<"$pid_file") alive." >&2
    exit 1
  fi
done
[[ ! -e "$state_dir/logs/gateway.launcher.env" ]] || { echo "Assertion failed: readiness identity failure left launcher state." >&2; exit 1; }
[[ -e "$state_dir/gateway.env" ]] || { echo "Assertion failed: readiness identity failure removed service state it did not own." >&2; exit 1; }

echo "runtime readiness identity deadline checks passed"
