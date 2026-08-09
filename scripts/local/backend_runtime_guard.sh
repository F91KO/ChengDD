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

backend_runtime_service_definitions() {
  printf '%s\n' \
    'gateway|cdd-gateway|CDD_GATEWAY_SERVER_PORT|8080|run_gateway.sh' \
    'auth-service|cdd-auth-service|CDD_AUTH_SERVER_PORT|8081|run_auth_service_mysql.sh' \
    'merchant-service|cdd-merchant-service|CDD_MERCHANT_SERVER_PORT|8082|run_merchant_service_mysql.sh' \
    'decoration-service|cdd-decoration-service|CDD_DECORATION_SERVER_PORT|8083|run_decoration_service_mysql.sh' \
    'product-service|cdd-product-service|CDD_PRODUCT_SERVER_PORT|8084|run_product_service_mysql.sh' \
    'order-service|cdd-order-service|CDD_ORDER_SERVER_PORT|8085|run_order_service_mysql.sh' \
    'marketing-service|cdd-marketing-service|CDD_MARKETING_SERVER_PORT|8086|run_marketing_service_mysql.sh' \
    'release-service|cdd-release-service|CDD_RELEASE_SERVER_PORT|8087|run_release_service_mysql.sh' \
    'report-service|cdd-report-service|CDD_REPORT_SERVER_PORT|8088|run_report_service_mysql.sh' \
    'config-service|cdd-config-service|CDD_CONFIG_SERVER_PORT|8089|run_config_service_mysql.sh'
}

backend_runtime_service_catalog() {
  local service_name module_name port_variable default_port launcher_name service_port
  local entries=()
  local seen_ports='|'

  while IFS='|' read -r service_name module_name port_variable default_port launcher_name; do
    service_port="${!port_variable-}"
    service_port="${service_port:-$default_port}"
    if [[ ! "$service_port" =~ ^[0-9]+$ ]] || (( 10#$service_port < 1 || 10#$service_port > 65535 )); then
      echo "${port_variable} must be an integer between 1 and 65535: ${service_port}" >&2
      return 1
    fi
    service_port=$((10#$service_port))
    if [[ "$seen_ports" == *"|${service_port}|"* ]]; then
      echo "Duplicate backend service port: ${service_port}" >&2
      return 1
    fi
    seen_ports+="${service_port}|"
    entries+=("${service_name}|${module_name}|${service_port}|${launcher_name}")
  done < <(backend_runtime_service_definitions)

  printf '%s\n' "${entries[@]}"
}

backend_runtime_service_port() {
  local requested_service="$1"
  local service_name _module_name service_port _launcher_name
  while IFS='|' read -r service_name _module_name service_port _launcher_name; do
    if [[ "$service_name" == "$requested_service" ]]; then
      printf '%s\n' "$service_port"
      return 0
    fi
  done < <(backend_runtime_service_catalog)
  echo "Unknown backend service: ${requested_service}" >&2
  return 1
}

wait_for_runtime_service_health() {
  local repo_root="$1"
  local service_name="$2"
  local module_name="$3"
  local service_port="$4"
  local launcher_pid="$5"
  local timeout_seconds="${CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS:-60}"
  local health_url="http://127.0.0.1:${service_port}/actuator/health"

  [[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
    echo "CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS must be a positive integer." >&2
    return 1
  }

  local deadline=$(( $(date +%s) + timeout_seconds ))
  local now remaining connect_timeout request_timeout
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  while :; do
    now="$(date +%s)"
    remaining=$(( deadline - now ))
    (( remaining > 0 )) || break
    connect_timeout=$(( remaining < 2 ? remaining : 2 ))
    request_timeout=$(( remaining < 5 ? remaining : 5 ))
    if read_backend_runtime_state "$state_file" \
      && [[ "$RUNTIME_STATE_SERVICE_NAME" == "$service_name" ]] \
      && [[ "$RUNTIME_STATE_MODULE_NAME" == "$module_name" ]] \
      && [[ "$RUNTIME_STATE_SERVICE_PORT" == "$service_port" ]] \
      && backend_runtime_process_matches_state "$service_name" "$module_name" "$service_port" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER" \
      && curl --silent --show-error --fail --connect-timeout "$connect_timeout" --max-time "$request_timeout" "$health_url" >/dev/null 2>&1; then
        return 0
    fi
    if ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      echo "${service_name} failed before passing health checks." >&2
      return 1
    fi
    (( $(date +%s) < deadline )) && sleep 1
  done

  echo "${service_name} did not pass health checks within ${timeout_seconds} seconds: ${health_url}" >&2
  return 1
}

backend_runtime_state_dir() {
  local repo_root="$1"
  local state_dir="${CDD_RUNTIME_STATE_DIR:-$repo_root/.local/backend-runtime}"
  if [[ "$state_dir" == "/" || "$state_dir" != /* ]]; then
    echo "CDD_RUNTIME_STATE_DIR must be a non-root absolute path." >&2
    return 1
  fi
  echo "$state_dir"
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

backend_runtime_port_listener_pids() {
  local service_port="$1"
  local inspector_output=""
  local inspector_status=0

  if [[ -n "${CDD_RUNTIME_PORT_INSPECTOR:-}" ]]; then
    [[ -x "$CDD_RUNTIME_PORT_INSPECTOR" ]] || {
      echo "CDD_RUNTIME_PORT_INSPECTOR must be executable: ${CDD_RUNTIME_PORT_INSPECTOR}" >&2
      return 1
    }
    "$CDD_RUNTIME_PORT_INSPECTOR" "$service_port"
    return
  fi

  if command -v lsof >/dev/null 2>&1; then
    inspector_output="$(lsof -nP -iTCP:"$service_port" -sTCP:LISTEN -t 2>/dev/null)" || inspector_status=$?
    if (( inspector_status != 0 && inspector_status != 1 )); then
      echo "Unable to inspect TCP port ${service_port} with lsof." >&2
      return 1
    fi
  elif command -v ss >/dev/null 2>&1; then
    inspector_output="$(ss -H -ltnp 2>/dev/null | awk -v port="$service_port" '
      $4 ~ (":" port "$") {
        found=1
        line=$0
        while (match(line, /pid=[0-9]+/)) {
          print substr(line, RSTART + 4, RLENGTH - 4)
          line=substr(line, RSTART + RLENGTH)
          proved=1
        }
      }
      END { if (found && !proved) exit 2 }
    ')" || inspector_status=$?
    if (( inspector_status != 0 )); then
      echo "Unable to prove TCP port ${service_port} ownership with ss." >&2
      return 1
    fi
  else
    echo "Cannot inspect TCP port ${service_port}: install lsof or ss." >&2
    return 1
  fi

  local listener_pid
  while IFS= read -r listener_pid; do
    [[ -n "$listener_pid" ]] || continue
    [[ "$listener_pid" =~ ^[0-9]+$ ]] || {
      echo "Port inspector returned an invalid pid for ${service_port}: ${listener_pid}" >&2
      return 1
    }
    printf '%s\n' "$listener_pid"
  done <<<"$inspector_output"
}

backend_runtime_assert_port_available() {
  local service_port="$1"
  local listener_pids
  if ! listener_pids="$(backend_runtime_port_listener_pids "$service_port" | sort -u)"; then
    return 1
  fi
  if [[ -n "$listener_pids" ]]; then
    echo "TCP port ${service_port} is already occupied by pid(s): $(printf '%s' "$listener_pids" | tr '\n' ' ')" >&2
    return 1
  fi
}

backend_runtime_listener_matches_process() {
  local service_port="$1"
  local expected_pid="$2"
  local listener_pids
  if ! listener_pids="$(backend_runtime_port_listener_pids "$service_port" | sort -u)"; then
    return 1
  fi
  [[ -n "$listener_pids" ]] || return 1

  local listener_pid
  local listener_count=0
  while IFS= read -r listener_pid; do
    [[ -n "$listener_pid" ]] || continue
    listener_count=$((listener_count + 1))
    [[ "$listener_pid" == "$expected_pid" ]] || return 1
  done <<<"$listener_pids"
  [[ "$listener_count" -eq 1 ]]
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
  local supplied_start_marker="${6:-}"
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
  process_start_marker="$supplied_start_marker"
  if [[ -z "$process_start_marker" ]]; then
    process_start_marker="$(wait_for_backend_runtime_process_start_marker "$service_pid")"
  fi
  if [[ -z "$process_start_marker" ]]; then
    echo "Unable to record process start marker for ${service_name} pid=${service_pid}." >&2
    return 1
  fi

  local state_file="${state_dir}/${service_name}.env"
  local temporary_state_file="${state_dir}/.${service_name}.env.tmp.$$"
  if ! cat > "$temporary_state_file" <<EOF
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
  then
    rm -f "$temporary_state_file"
    return 1
  fi
  if ! backend_runtime_process_matches_state "$service_name" "$module_name" "$service_port" "$service_pid" "$process_start_marker" \
    || ! backend_runtime_listener_matches_process "$service_port" "$service_pid"; then
    rm -f "$temporary_state_file"
    echo "Refusing to publish stale or unowned runtime state for ${service_name} pid=${service_pid}." >&2
    return 1
  fi
  if ! mv -f "$temporary_state_file" "$state_file"; then
    rm -f "$temporary_state_file"
    return 1
  fi
}
