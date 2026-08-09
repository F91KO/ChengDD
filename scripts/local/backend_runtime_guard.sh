#!/usr/bin/env bash
set -euo pipefail

configure_backend_runtime() {
  runtime_env="${CDD_ENV:-local}"
  runtime_config_mode="${CDD_CONFIG_MODE:-file}"

  case "$runtime_env" in
    local|dev|test|prod) ;;
    *)
      echo "CDD_ENV must be one of local, dev, test, prod: ${runtime_env}" >&2
      return 1
      ;;
  esac

  case "$runtime_config_mode" in
    file|nacos) ;;
    *)
      echo "CDD_CONFIG_MODE must be file or nacos: ${runtime_config_mode}" >&2
      return 1
      ;;
  esac

  export CDD_ENV="$runtime_env"
  export CDD_CONFIG_MODE="$runtime_config_mode"
}

backend_runtime_service_catalog() {
  printf '%s\n' \
    'gateway|cdd-gateway|8080|run_gateway.sh' \
    'auth-service|cdd-auth-service|8081|run_auth_service_mysql.sh' \
    'merchant-service|cdd-merchant-service|8082|run_merchant_service_mysql.sh' \
    'decoration-service|cdd-decoration-service|8083|run_decoration_service_mysql.sh' \
    'product-service|cdd-product-service|8084|run_product_service_mysql.sh' \
    'order-service|cdd-order-service|8085|run_order_service_mysql.sh' \
    'marketing-service|cdd-marketing-service|8086|run_marketing_service_mysql.sh' \
    'release-service|cdd-release-service|8087|run_release_service_mysql.sh' \
    'report-service|cdd-report-service|8088|run_report_service_mysql.sh' \
    'config-service|cdd-config-service|8089|run_config_service_mysql.sh'
}

wait_for_runtime_service_health() {
  local service_name="$1"
  local service_port="$2"
  local launcher_pid="$3"
  local timeout_seconds="${CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS:-60}"
  local health_url="http://127.0.0.1:${service_port}/actuator/health"

  [[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
    echo "CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS must be a positive integer." >&2
    return 1
  }

  local deadline=$(( $(date +%s) + timeout_seconds ))
  while (( $(date +%s) < deadline )); do
    if curl --silent --show-error --fail --connect-timeout 2 --max-time 5 "$health_url" >/dev/null 2>&1; then
      return 0
    fi
    if ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      echo "${service_name} failed before passing health checks." >&2
      return 1
    fi
    sleep 1
  done

  echo "${service_name} did not pass health checks within ${timeout_seconds} seconds: ${health_url}" >&2
  return 1
}

backend_runtime_state_dir() {
  local repo_root="$1"
  echo "$repo_root/.local/backend-runtime"
}

backend_runtime_state_file() {
  local repo_root="$1"
  local service_name="$2"
  printf '%s/%s.env\n' "$(backend_runtime_state_dir "$repo_root")" "$service_name"
}

backend_runtime_launcher_state_file() {
  local repo_root="$1"
  local service_name="$2"
  printf '%s/logs/%s.launcher.env\n' "$(backend_runtime_state_dir "$repo_root")" "$service_name"
}

backend_runtime_process_start_marker() {
  local service_pid="$1"
  if [[ -r "/proc/${service_pid}/stat" ]]; then
    awk '{print $22}' "/proc/${service_pid}/stat"
    return 0
  fi
  ps -p "$service_pid" -o lstart= 2>/dev/null | tr -d '[:space:]'
}

backend_runtime_process_command() {
  local service_pid="$1"
  ps -p "$service_pid" -o command= 2>/dev/null
}

wait_for_backend_runtime_process_start_marker() {
  local service_pid="$1"
  local marker=""
  for _ in $(seq 1 10); do
    marker="$(backend_runtime_process_start_marker "$service_pid")"
    if [[ -n "$marker" ]]; then
      printf '%s\n' "$marker"
      return 0
    fi
    kill -0 "$service_pid" >/dev/null 2>&1 || return 1
    sleep 0.1
  done
  return 1
}

read_backend_runtime_state() {
  local state_file="$1"
  RUNTIME_STATE_SERVICE_NAME=""
  RUNTIME_STATE_MODULE_NAME=""
  RUNTIME_STATE_SERVICE_PORT=""
  RUNTIME_STATE_SERVICE_PID=""
  RUNTIME_STATE_PROCESS_START_MARKER=""
  RUNTIME_STATE_GIT_HEAD=""
  RUNTIME_STATE_BACKEND_FINGERPRINT=""
  RUNTIME_STATE_STARTED_AT=""
  RUNTIME_STATE_STARTED_AT_TEXT=""

  [[ -f "$state_file" ]] || return 1

  local key value
  while IFS='=' read -r key value || [[ -n "$key" ]]; do
    case "$key" in
      SERVICE_NAME) RUNTIME_STATE_SERVICE_NAME="$value" ;;
      MODULE_NAME) RUNTIME_STATE_MODULE_NAME="$value" ;;
      SERVICE_PORT) RUNTIME_STATE_SERVICE_PORT="$value" ;;
      SERVICE_PID) RUNTIME_STATE_SERVICE_PID="$value" ;;
      PROCESS_START_MARKER) RUNTIME_STATE_PROCESS_START_MARKER="$value" ;;
      GIT_HEAD) RUNTIME_STATE_GIT_HEAD="$value" ;;
      BACKEND_FINGERPRINT) RUNTIME_STATE_BACKEND_FINGERPRINT="$value" ;;
      STARTED_AT) RUNTIME_STATE_STARTED_AT="$value" ;;
      STARTED_AT_TEXT) RUNTIME_STATE_STARTED_AT_TEXT="$value" ;;
      ''|'#'*) ;;
      *) return 1 ;;
    esac
  done < "$state_file"

  [[ "$RUNTIME_STATE_SERVICE_PID" =~ ^[0-9]+$ ]] || return 1
  [[ "$RUNTIME_STATE_SERVICE_PORT" =~ ^[0-9]+$ ]] || return 1
  [[ -n "$RUNTIME_STATE_PROCESS_START_MARKER" ]] || return 1
}

read_backend_runtime_launcher_state() {
  local state_file="$1"
  RUNTIME_LAUNCHER_SERVICE_NAME=""
  RUNTIME_LAUNCHER_NAME=""
  RUNTIME_LAUNCHER_PID=""
  RUNTIME_LAUNCHER_START_MARKER=""

  [[ -f "$state_file" ]] || return 1

  local key value
  while IFS='=' read -r key value || [[ -n "$key" ]]; do
    case "$key" in
      SERVICE_NAME) RUNTIME_LAUNCHER_SERVICE_NAME="$value" ;;
      LAUNCHER_NAME) RUNTIME_LAUNCHER_NAME="$value" ;;
      LAUNCHER_PID) RUNTIME_LAUNCHER_PID="$value" ;;
      PROCESS_START_MARKER) RUNTIME_LAUNCHER_START_MARKER="$value" ;;
      ''|'#'*) ;;
      *) return 1 ;;
    esac
  done < "$state_file"

  [[ "$RUNTIME_LAUNCHER_PID" =~ ^[0-9]+$ ]] || return 1
  [[ -n "$RUNTIME_LAUNCHER_START_MARKER" ]] || return 1
}

backend_runtime_process_matches_state() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local service_pid="$4"
  local start_marker="$5"

  kill -0 "$service_pid" >/dev/null 2>&1 || return 1
  [[ "$(backend_runtime_process_start_marker "$service_pid")" == "$start_marker" ]] || return 1

  local process_command
  process_command="$(backend_runtime_process_command "$service_pid")"
  [[ "$process_command" == *java* ]] || return 1
  [[ "$process_command" == *"${module_name}-0.1.0-SNAPSHOT.jar"* ]] || return 1
  [[ "$process_command" == *"--server.port=${service_port}"* ]] || return 1
  [[ "$service_name" != "" ]]
}

backend_runtime_launcher_matches_state() {
  local service_name="$1"
  local launcher_name="$2"
  local launcher_pid="$3"
  local start_marker="$4"

  kill -0 "$launcher_pid" >/dev/null 2>&1 || return 1
  [[ "$start_marker" != "unavailable" ]] || return 1
  [[ "$(backend_runtime_process_start_marker "$launcher_pid")" == "$start_marker" ]] || return 1
  local process_command
  process_command="$(backend_runtime_process_command "$launcher_pid")"
  [[ "$process_command" == *"${launcher_name}"* ]] || return 1
  [[ "$service_name" != "" ]]
}

remove_backend_runtime_state() {
  local repo_root="$1"
  local service_name="$2"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  rm -f "$state_file"
  rm -f "$(backend_runtime_launcher_state_file "$repo_root" "$service_name")"
}

record_backend_launcher_state() {
  local repo_root="$1"
  local service_name="$2"
  local launcher_name="$3"
  local launcher_pid="$4"
  local state_dir
  state_dir="$(backend_runtime_state_dir "$repo_root")"
  mkdir -p "$state_dir/logs"

  local start_marker
  start_marker="$(backend_runtime_process_start_marker "$launcher_pid" || true)"
  start_marker="${start_marker:-unavailable}"

  cat > "$(backend_runtime_launcher_state_file "$repo_root" "$service_name")" <<EOF
SERVICE_NAME=${service_name}
LAUNCHER_NAME=${launcher_name}
LAUNCHER_PID=${launcher_pid}
PROCESS_START_MARKER=${start_marker}
EOF
}

compute_backend_runtime_fingerprint() {
  local repo_root="$1"
  (
    cd "$repo_root"
    git rev-parse HEAD 2>/dev/null || echo "NO_GIT_HEAD"
    git status --porcelain --untracked-files=no -- cdd-parent config db scripts/local README.md 2>/dev/null || true
  ) | shasum -a 256 | awk '{print $1}'
}

record_backend_runtime_state() {
  local repo_root="$1"
  local service_name="$2"
  local module_name="$3"
  local service_port="$4"
  local service_pid="${5:-}"
  local state_dir
  state_dir="$(backend_runtime_state_dir "$repo_root")"
  mkdir -p "$state_dir"

  local git_head
  git_head="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo "unknown")"
  local fingerprint
  fingerprint="$(compute_backend_runtime_fingerprint "$repo_root")"
  local started_at
  started_at="$(date +%s)"
  local started_at_text
  started_at_text="$(date '+%Y-%m-%dT%H:%M:%S')"
  local process_start_marker
  process_start_marker="$(wait_for_backend_runtime_process_start_marker "$service_pid")"
  if [[ -z "$process_start_marker" ]]; then
    echo "Unable to record process start marker for ${service_name} pid=${service_pid}." >&2
    return 1
  fi

  cat > "${state_dir}/${service_name}.env" <<EOF
SERVICE_NAME=${service_name}
MODULE_NAME=${module_name}
SERVICE_PORT=${service_port}
SERVICE_PID=${service_pid}
PROCESS_START_MARKER=${process_start_marker}
GIT_HEAD=${git_head}
BACKEND_FINGERPRINT=${fingerprint}
STARTED_AT=${started_at}
STARTED_AT_TEXT=${started_at_text}
EOF
}
