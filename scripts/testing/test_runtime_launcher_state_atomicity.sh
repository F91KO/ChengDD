#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-launcher-atomic.XXXXXX)"
fixture_bin="$fixture_root/bin"
fixture_scripts="$fixture_root/scripts"
fixture_launchers="$fixture_root/launchers"
state_dir="$fixture_root/runtime-state"
launcher_pid_file="$fixture_root/launcher.pid"
child_pid_file="$fixture_root/child.pid"
prior_state_file="$fixture_root/prior-launcher.env"
mkdir -p "$fixture_bin" "$fixture_scripts" "$fixture_launchers" "$state_dir/logs"

cleanup() {
  local code=$?
  for pid_file in "$launcher_pid_file" "$child_pid_file"; do
    [[ -s "$pid_file" ]] || continue
    kill -KILL "$(<"$pid_file")" >/dev/null 2>&1 || true
  done
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

printf '%s\n' '#!/usr/bin/env bash' 'exit 1' >"$fixture_bin/lsof"
cat >"$fixture_bin/mv" <<'EOF'
#!/usr/bin/env bash
case "${*: -1}" in
  *.launcher.env) exit 73 ;;
esac
exec /bin/mv "$@"
EOF
cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
request="$*"
pid=""
previous=""
for argument in "$@"; do if [[ "$previous" == "-p" ]]; then pid="$argument"; fi; previous="$argument"; done
if [[ "$request" == *"lstart="* ]]; then
  while [[ ! -s "$CDD_TEST_PRIOR_STATE_FILE" ]]; do sleep 0.01; done
  printf 'marker-%s\n' "$pid"
  exit 0
fi
if [[ "$request" == *"state="* ]]; then printf 'T\n'; exit 0; fi
if [[ "$request" == *"command="* ]]; then printf 'bash %s/run_gateway.sh\n' "$CDD_RUNTIME_LAUNCHER_DIR"; exit 0; fi
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
printf '%s\n' "$!" >"$CDD_TEST_CHILD_PID_FILE"
cat >"$CDD_TEST_PRIOR_STATE_FILE" <<STATE
# prior-valid-state
SERVICE_NAME=gateway
LAUNCHER_NAME=run_gateway.sh
LAUNCHER_PID=$$
PROCESS_START_MARKER=marker-$$
STATE
cp "$CDD_TEST_PRIOR_STATE_FILE" "$CDD_RUNTIME_STATE_DIR/logs/gateway.launcher.env"
while :; do :; done
EOF
chmod +x "$fixture_launchers/run_gateway.sh"
for launcher in run_auth_service_mysql.sh run_merchant_service_mysql.sh run_decoration_service_mysql.sh run_product_service_mysql.sh run_order_service_mysql.sh run_marketing_service_mysql.sh run_release_service_mysql.sh run_report_service_mysql.sh run_config_service_mysql.sh; do
  printf '%s\n' '#!/usr/bin/env bash' 'exit 99' >"$fixture_launchers/$launcher"
  chmod +x "$fixture_launchers/$launcher"
done

if PATH="$fixture_bin:$PATH" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_UP_INFRA_SCRIPT="$fixture_scripts/up.sh" \
  CDD_RUNTIME_MIGRATE_SCRIPT="$fixture_scripts/migrate.sh" \
  CDD_RUNTIME_LAUNCHER_DIR="$fixture_launchers" \
  CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS=7 \
  CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS=5 \
  CDD_TEST_LAUNCHER_PID_FILE="$launcher_pid_file" \
  CDD_TEST_CHILD_PID_FILE="$child_pid_file" \
  CDD_TEST_PRIOR_STATE_FILE="$prior_state_file" \
  bash "$repo_root/scripts/local/run_all_services_mysql.sh" >/dev/null 2>&1; then
  echo "Assertion failed: launcher-state mv failure returned success." >&2
  exit 1
fi

cmp -s "$prior_state_file" "$state_dir/logs/gateway.launcher.env" || {
  echo "Assertion failed: launcher-state publication failure replaced prior valid state." >&2
  exit 1
}
if find "$state_dir/logs" -name '*.tmp.*' -print -quit | grep -q .; then
  echo "Assertion failed: launcher-state publication failure left a temporary file." >&2
  exit 1
fi
for pid_file in "$launcher_pid_file" "$child_pid_file"; do
  [[ -s "$pid_file" ]] || { echo "Assertion failed: atomic launcher fixture did not record a pid." >&2; exit 1; }
  if kill -0 "$(<"$pid_file")" >/dev/null 2>&1; then
    echo "Assertion failed: launcher-state publication failure left pid $(<"$pid_file") alive." >&2
    exit 1
  fi
done

echo "runtime launcher-state atomicity checks passed"
