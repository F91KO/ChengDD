#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-existing-state.XXXXXX)"
fixture_bin="$fixture_root/bin"
fixture_scripts="$fixture_root/scripts"
fixture_launchers="$fixture_root/launchers"
state_dir="$fixture_root/runtime-state"
trace_file="$fixture_root/trace"
mkdir -p "$fixture_bin" "$fixture_scripts" "$fixture_launchers" "$state_dir/logs"

cleanup() {
  local code=$?
  for fixture_pid in "${old_java_pid:-}" "${old_launcher_pid:-}"; do
    [[ -n "$fixture_pid" ]] || continue
    kill -KILL "$fixture_pid" >/dev/null 2>&1 || true
  done
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

bash -c 'while :; do :; done' &
old_java_pid=$!
bash -c 'while :; do :; done' &
old_launcher_pid=$!
expected_jar_path="$repo_root/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar"
expected_java_path="$fixture_root/java"

cat >"$state_dir/gateway.env" <<EOF
SERVICE_NAME=gateway
MODULE_NAME=cdd-gateway
SERVICE_PORT=19080
SERVICE_PID=${old_java_pid}
PROCESS_START_MARKER=marker-${old_java_pid}
JAVA_PATH=${expected_java_path}
JAR_PATH=${expected_jar_path}
GIT_HEAD=fixture
BACKEND_FINGERPRINT=fixture
STARTED_AT=0
STARTED_AT_TEXT=fixture
EOF
cat >"$state_dir/logs/gateway.launcher.env" <<EOF
SERVICE_NAME=gateway
LAUNCHER_NAME=run_gateway.sh
LAUNCHER_PID=${old_launcher_pid}
PROCESS_START_MARKER=marker-${old_launcher_pid}
EOF
service_state_before="$(shasum -a 256 "$state_dir/gateway.env")"
launcher_state_before="$(shasum -a 256 "$state_dir/logs/gateway.launcher.env")"

cat >"$fixture_bin/ps" <<EOF
#!/usr/bin/env bash
request="\$*"
pid=""
previous=""
for argument in "\$@"; do if [[ "\$previous" == "-p" ]]; then pid="\$argument"; fi; previous="\$argument"; done
if [[ "\$request" == *"lstart="* ]]; then printf 'marker-%s\\n' "\$pid"; exit 0; fi
if [[ "\$request" == *"command="* && "\$pid" == "${old_java_pid}" ]]; then printf '%s\\n' '${expected_java_path} -jar ${expected_jar_path} --server.port=19080'; exit 0; fi
if [[ "\$request" == *"command="* && "\$pid" == "${old_launcher_pid}" ]]; then printf '%s\\n' 'bash ${fixture_launchers}/run_gateway.sh'; exit 0; fi
exit 1
EOF
chmod +x "$fixture_bin/ps"
printf '%s\n' '#!/usr/bin/env bash' 'exit 1' >"$fixture_bin/lsof"
printf '%s\n' '#!/usr/bin/env bash' 'echo infra >>"$CDD_TEST_TRACE"' >"$fixture_scripts/up.sh"
printf '%s\n' '#!/usr/bin/env bash' 'echo migrate >>"$CDD_TEST_TRACE"' >"$fixture_scripts/migrate.sh"
printf '%s\n' '#!/usr/bin/env bash' 'echo publish >>"$CDD_TEST_TRACE"' >"$fixture_scripts/publish.sh"
for file in "$fixture_bin/lsof" "$fixture_scripts"/*.sh; do chmod +x "$file"; done
for launcher in run_gateway.sh run_auth_service_mysql.sh run_merchant_service_mysql.sh run_decoration_service_mysql.sh run_product_service_mysql.sh run_order_service_mysql.sh run_marketing_service_mysql.sh run_release_service_mysql.sh run_report_service_mysql.sh run_config_service_mysql.sh; do
  printf '%s\n' '#!/usr/bin/env bash' 'echo launch >>"$CDD_TEST_TRACE"' 'while :; do :; done' >"$fixture_launchers/$launcher"
  chmod +x "$fixture_launchers/$launcher"
done

if PATH="$fixture_bin:$PATH" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_UP_INFRA_SCRIPT="$fixture_scripts/up.sh" \
  CDD_RUNTIME_MIGRATE_SCRIPT="$fixture_scripts/migrate.sh" \
  CDD_RUNTIME_PUBLISH_SCRIPT="$fixture_scripts/publish.sh" \
  CDD_RUNTIME_LAUNCHER_DIR="$fixture_launchers" \
  CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS=1 \
  CDD_TEST_TRACE="$trace_file" \
  bash "$repo_root/scripts/local/run_all_services_mysql.sh" >/dev/null 2>&1; then
  echo "Assertion failed: a live service recorded on an old override must block run-all." >&2
  exit 1
fi
[[ ! -s "$trace_file" ]] || {
  echo "Assertion failed: old-override state was inspected after infrastructure or launch." >&2
  exit 1
}
[[ "$service_state_before" == "$(shasum -a 256 "$state_dir/gateway.env")" ]] || {
  echo "Assertion failed: old-override service state was deleted or overwritten." >&2
  exit 1
}
[[ "$launcher_state_before" == "$(shasum -a 256 "$state_dir/logs/gateway.launcher.env")" ]] || {
  echo "Assertion failed: old-override launcher state was deleted or overwritten." >&2
  exit 1
}
kill -0 "$old_java_pid" >/dev/null 2>&1 && kill -0 "$old_launcher_pid" >/dev/null 2>&1 || {
  echo "Assertion failed: state preflight signalled an existing owned runtime." >&2
  exit 1
}

echo "runtime existing-state safety checks passed"
