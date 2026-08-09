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
    if [[ ! "$service_port" =~ ^[1-9][0-9]{0,4}$ ]]; then
      echo "${port_variable} must be an integer between 1 and 65535: ${service_port}" >&2
      return 1
    fi
    if (( service_port > 65535 )); then
      echo "${port_variable} must be an integer between 1 and 65535: ${service_port}" >&2
      return 1
    fi
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
  local deadline="$6"
  local health_url="http://127.0.0.1:${service_port}/actuator/health"
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
      && backend_runtime_process_matches_state_before_deadline "$deadline" "$service_name" "$module_name" "$service_port" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER" "$RUNTIME_STATE_JAVA_PATH" "$RUNTIME_STATE_JAR_PATH" \
      && curl --silent --show-error --fail --connect-timeout "$connect_timeout" --max-time "$request_timeout" "$health_url" >/dev/null 2>&1; then
        return 0
    fi
    if ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      echo "${service_name} failed before passing health checks." >&2
      return 1
    fi
    (( $(date +%s) < deadline )) && sleep 1
  done

  echo "${service_name} did not publish owned healthy state before its readiness deadline: ${health_url}" >&2
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

backend_runtime_process_argv() {
  local service_pid="$1"
  if [[ -r "/proc/${service_pid}/cmdline" ]]; then
    local argument
    while IFS= read -r -d '' argument; do
      printf '%s\n' "$argument"
    done < "/proc/${service_pid}/cmdline"
    return 0
  fi

  local process_command
  process_command="$(ps -ww -p "$service_pid" -o command= 2>/dev/null)" || return 1
  [[ -n "$process_command" ]] || return 1
  local process_arguments=()
  read -r -a process_arguments <<<"$process_command"
  printf '%s\n' "${process_arguments[@]}"
}

backend_runtime_java_argv_matches() {
  local expected_java_path="$1"
  local expected_jar_path="$2"
  local expected_port="$3"
  local process_arguments=()
  local argument
  while IFS= read -r argument; do
    process_arguments+=("$argument")
  done

  [[ ${#process_arguments[@]} -gt 0 ]] || return 1
  [[ "${process_arguments[0]}" == "$expected_java_path" ]] || return 1

  local jar_matches=0
  local port_matches=0
  local total_port_arguments=0
  local index
  for ((index=0; index<${#process_arguments[@]}; index++)); do
    if [[ "${process_arguments[$index]}" == "-jar" ]]; then
      (( index + 1 < ${#process_arguments[@]} )) || return 1
      [[ "${process_arguments[$((index + 1))]}" == "$expected_jar_path" ]] || return 1
      jar_matches=$((jar_matches + 1))
    fi
    if [[ "${process_arguments[$index]}" == --server.port=* ]]; then
      total_port_arguments=$((total_port_arguments + 1))
      if [[ "${process_arguments[$index]}" == "--server.port=${expected_port}" ]]; then
        port_matches=$((port_matches + 1))
      fi
    fi
  done
  [[ "$jar_matches" -eq 1 && "$total_port_arguments" -eq 1 && "$port_matches" -eq 1 ]]
}

backend_runtime_run_bounded_command() {
  local deadline="$1"
  local output_file="$2"
  local error_file="$3"
  shift 3
  (( $(date +%s) < deadline )) || return 124

  "$@" >"$output_file" 2>"$error_file" &
  local command_pid=$!
  while kill -0 "$command_pid" >/dev/null 2>&1; do
    if (( $(date +%s) >= deadline )); then
      kill -TERM "$command_pid" >/dev/null 2>&1 || true
      sleep 0.1
      kill -KILL "$command_pid" >/dev/null 2>&1 || true
      wait "$command_pid" >/dev/null 2>&1 || true
      return 124
    fi
    sleep 0.05
  done
  local command_status=0
  wait "$command_pid" || command_status=$?
  return "$command_status"
}

backend_runtime_capture_bounded_command() {
  local deadline="$1"
  shift
  local output_file error_file command_status=0
  output_file="$(mktemp "${TMPDIR:-/tmp}/chengdd-bounded-output.XXXXXX")"
  error_file="$(mktemp "${TMPDIR:-/tmp}/chengdd-bounded-error.XXXXXX")"
  backend_runtime_run_bounded_command "$deadline" "$output_file" "$error_file" "$@" || command_status=$?
  if (( command_status == 0 )); then
    printf '%s' "$(<"$output_file")"
  fi
  rm -f "$output_file" "$error_file"
  return "$command_status"
}

backend_runtime_process_start_marker_before_deadline() {
  local deadline="$1"
  local service_pid="$2"
  local marker
  if [[ -r "/proc/${service_pid}/stat" ]]; then
    marker="$(backend_runtime_capture_bounded_command "$deadline" awk '{print $22}' "/proc/${service_pid}/stat")" || return 1
  else
    marker="$(backend_runtime_capture_bounded_command "$deadline" ps -p "$service_pid" -o lstart=)" || return 1
    marker="${marker//[[:space:]]/}"
  fi
  [[ -n "$marker" ]] || return 1
  printf '%s\n' "$marker"
}

backend_runtime_process_argv_before_deadline() {
  local deadline="$1"
  local service_pid="$2"
  if [[ -r "/proc/${service_pid}/cmdline" ]]; then
    local argument
    while IFS= read -r -d '' argument; do
      printf '%s\n' "$argument"
    done < "/proc/${service_pid}/cmdline"
    return 0
  fi

  local process_command
  process_command="$(backend_runtime_capture_bounded_command "$deadline" ps -ww -p "$service_pid" -o command=)" || return 1
  [[ -n "$process_command" ]] || return 1
  local process_arguments=()
  read -r -a process_arguments <<<"$process_command"
  printf '%s\n' "${process_arguments[@]}"
}

backend_runtime_parse_ss_listener_pids() {
  local service_port="$1"
  awk -v port="$service_port" '
    $4 ~ (":" port "$") {
      row_proved=0
      line=$0
      while (match(line, /pid=[0-9]+/)) {
        print substr(line, RSTART + 4, RLENGTH - 4)
        line=substr(line, RSTART + RLENGTH)
        row_proved=1
      }
      if (!row_proved) unproved=1
    }
    END { if (unproved) exit 2 }
  '
}

backend_runtime_port_listener_pids() {
  local service_port="$1"
  local deadline="${2:-}"
  local inspector_output=""
  local inspector_status=0
  local inspection_timeout="${CDD_RUNTIME_PORT_INSPECTION_TIMEOUT_SECONDS:-5}"
  [[ "$inspection_timeout" =~ ^[1-9][0-9]{0,4}$ ]] || {
    echo "CDD_RUNTIME_PORT_INSPECTION_TIMEOUT_SECONDS must be a positive integer." >&2
    return 1
  }
  [[ -n "$deadline" ]] || deadline=$(( $(date +%s) + inspection_timeout ))
  (( $(date +%s) < deadline )) || return 1

  local output_file error_file
  output_file="$(mktemp "${TMPDIR:-/tmp}/chengdd-port-inspector-output.XXXXXX")"
  error_file="$(mktemp "${TMPDIR:-/tmp}/chengdd-port-inspector-error.XXXXXX")"

  if [[ -n "${CDD_RUNTIME_PORT_INSPECTOR:-}" ]]; then
    [[ -x "$CDD_RUNTIME_PORT_INSPECTOR" ]] || {
      echo "CDD_RUNTIME_PORT_INSPECTOR must be executable: ${CDD_RUNTIME_PORT_INSPECTOR}" >&2
      rm -f "$output_file" "$error_file"
      return 1
    }
    backend_runtime_run_bounded_command "$deadline" "$output_file" "$error_file" "$CDD_RUNTIME_PORT_INSPECTOR" "$service_port" || inspector_status=$?
    inspector_output="$(<"$output_file")"
  fi

  if [[ -z "${CDD_RUNTIME_PORT_INSPECTOR:-}" ]] && command -v lsof >/dev/null 2>&1; then
    backend_runtime_run_bounded_command "$deadline" "$output_file" "$error_file" lsof -nP -iTCP:"$service_port" -sTCP:LISTEN -t || inspector_status=$?
    inspector_output="$(<"$output_file")"
    if (( inspector_status != 0 && inspector_status != 1 )); then
      echo "Unable to inspect TCP port ${service_port} with lsof." >&2
      rm -f "$output_file" "$error_file"
      return 1
    fi
    inspector_status=0
  elif [[ -z "${CDD_RUNTIME_PORT_INSPECTOR:-}" ]] && command -v ss >/dev/null 2>&1; then
    backend_runtime_run_bounded_command "$deadline" "$output_file" "$error_file" ss -H -ltnp || inspector_status=$?
    if (( inspector_status != 0 )); then
      echo "Unable to inspect TCP port ${service_port} with ss." >&2
      rm -f "$output_file" "$error_file"
      return 1
    fi
    inspector_output="$(backend_runtime_parse_ss_listener_pids "$service_port" <"$output_file")" || inspector_status=$?
    if (( inspector_status != 0 )); then
      echo "Unable to prove every TCP port ${service_port} listener with ss." >&2
      rm -f "$output_file" "$error_file"
      return 1
    fi
  elif [[ -z "${CDD_RUNTIME_PORT_INSPECTOR:-}" ]]; then
    echo "Cannot inspect TCP port ${service_port}: install lsof or ss." >&2
    rm -f "$output_file" "$error_file"
    return 1
  fi

  if (( inspector_status != 0 )); then
    echo "TCP port ${service_port} inspection failed or exceeded its deadline." >&2
    rm -f "$output_file" "$error_file"
    return 1
  fi
  rm -f "$output_file" "$error_file"

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
  local deadline="${2:-}"
  local listener_pids
  if ! listener_pids="$(backend_runtime_port_listener_pids "$service_port" "$deadline" | sort -u)"; then
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
  local deadline="${3:-}"
  local listener_pids
  if ! listener_pids="$(backend_runtime_port_listener_pids "$service_port" "$deadline" | sort -u)"; then
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

wait_for_backend_runtime_process_start_marker_before_deadline() {
  local service_pid="$1"
  local deadline="$2"
  local marker=""
  while (( $(date +%s) < deadline )); do
    marker="$(backend_runtime_process_start_marker_before_deadline "$deadline" "$service_pid")" || marker=""
    if [[ -n "$marker" ]]; then
      printf '%s\n' "$marker"
      return 0
    fi
    kill -0 "$service_pid" >/dev/null 2>&1 || return 1
    sleep 0.05
  done
  return 1
}

reset_backend_runtime_state_globals() {
  RUNTIME_STATE_SERVICE_NAME=""
  RUNTIME_STATE_MODULE_NAME=""
  RUNTIME_STATE_SERVICE_PORT=""
  RUNTIME_STATE_SERVICE_PID=""
  RUNTIME_STATE_PROCESS_START_MARKER=""
  RUNTIME_STATE_JAVA_PATH=""
  RUNTIME_STATE_JAR_PATH=""
  RUNTIME_STATE_GIT_HEAD=""
  RUNTIME_STATE_BACKEND_FINGERPRINT=""
  RUNTIME_STATE_STARTED_AT=""
  RUNTIME_STATE_STARTED_AT_TEXT=""
}

read_backend_runtime_state() {
  local state_file="$1"
  reset_backend_runtime_state_globals

  [[ -f "$state_file" ]] || return 1

  local key value
  while IFS='=' read -r key value || [[ -n "$key" ]]; do
    case "$key" in
      SERVICE_NAME) RUNTIME_STATE_SERVICE_NAME="$value" ;;
      MODULE_NAME) RUNTIME_STATE_MODULE_NAME="$value" ;;
      SERVICE_PORT) RUNTIME_STATE_SERVICE_PORT="$value" ;;
      SERVICE_PID) RUNTIME_STATE_SERVICE_PID="$value" ;;
      PROCESS_START_MARKER) RUNTIME_STATE_PROCESS_START_MARKER="$value" ;;
      JAVA_PATH) RUNTIME_STATE_JAVA_PATH="$value" ;;
      JAR_PATH) RUNTIME_STATE_JAR_PATH="$value" ;;
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
  [[ "$RUNTIME_STATE_JAVA_PATH" == /* && "$RUNTIME_STATE_JAR_PATH" == /* ]] || return 1
}

reset_backend_runtime_launcher_state_globals() {
  RUNTIME_LAUNCHER_SERVICE_NAME=""
  RUNTIME_LAUNCHER_NAME=""
  RUNTIME_LAUNCHER_PID=""
  RUNTIME_LAUNCHER_START_MARKER=""
}

read_backend_runtime_launcher_state() {
  local state_file="$1"
  reset_backend_runtime_launcher_state_globals

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
  local expected_java_path="$6"
  local expected_jar_path="$7"

  kill -0 "$service_pid" >/dev/null 2>&1 || return 1
  [[ "$(backend_runtime_process_start_marker "$service_pid")" == "$start_marker" ]] || return 1

  [[ "$expected_java_path" == /* && "$expected_jar_path" == /* ]] || return 1
  [[ "$expected_jar_path" == *"/${module_name}-0.1.0-SNAPSHOT.jar" ]] || return 1
  if [[ ! -r "/proc/${service_pid}/cmdline" && ( "$expected_java_path" == *[[:space:]]* || "$expected_jar_path" == *[[:space:]]* ) ]]; then
    echo "Cannot prove exact Java argv with whitespace paths on this platform." >&2
    return 1
  fi
  backend_runtime_process_argv "$service_pid" | backend_runtime_java_argv_matches "$expected_java_path" "$expected_jar_path" "$service_port" || return 1
  [[ "$service_name" != "" ]]
}

backend_runtime_process_matches_state_before_deadline() {
  local deadline="$1"
  shift
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local service_pid="$4"
  local start_marker="$5"
  local expected_java_path="$6"
  local expected_jar_path="$7"

  kill -0 "$service_pid" >/dev/null 2>&1 || return 1
  local observed_marker
  observed_marker="$(backend_runtime_process_start_marker_before_deadline "$deadline" "$service_pid")" || return 1
  [[ "$observed_marker" == "$start_marker" ]] || return 1
  [[ "$expected_java_path" == /* && "$expected_jar_path" == /* ]] || return 1
  [[ "$expected_jar_path" == *"/${module_name}-0.1.0-SNAPSHOT.jar" ]] || return 1
  if [[ ! -r "/proc/${service_pid}/cmdline" && ( "$expected_java_path" == *[[:space:]]* || "$expected_jar_path" == *[[:space:]]* ) ]]; then
    echo "Cannot prove exact Java argv with whitespace paths on this platform." >&2
    return 1
  fi
  local process_argv
  process_argv="$(backend_runtime_process_argv_before_deadline "$deadline" "$service_pid")" || return 1
  printf '%s\n' "$process_argv" | backend_runtime_java_argv_matches "$expected_java_path" "$expected_jar_path" "$service_port" || return 1
  [[ -n "$service_name" ]]
}

backend_runtime_default_inspection_deadline() {
  local inspection_timeout="${CDD_RUNTIME_PROCESS_INSPECTION_TIMEOUT_SECONDS:-5}"
  [[ "$inspection_timeout" =~ ^[1-9][0-9]{0,4}$ ]] || {
    echo "CDD_RUNTIME_PROCESS_INSPECTION_TIMEOUT_SECONDS must be a positive integer." >&2
    return 1
  }
  printf '%s\n' "$(( $(date +%s) + inspection_timeout ))"
}

backend_runtime_validate_existing_service_state() {
  local repo_root="$1"
  local service_name="$2"
  local module_name="$3"
  local expected_jar_path="$4"
  local deadline="$5"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  [[ -f "$state_file" ]] || return 0

  if ! read_backend_runtime_state "$state_file"; then
    echo "Refusing malformed existing service state: ${state_file}" >&2
    return 1
  fi
  if [[ "$RUNTIME_STATE_SERVICE_NAME" != "$service_name" || "$RUNTIME_STATE_MODULE_NAME" != "$module_name" || "$RUNTIME_STATE_JAR_PATH" != "$expected_jar_path" ]]; then
    echo "Refusing mismatched existing service state: ${state_file}" >&2
    return 1
  fi
  if kill -0 "$RUNTIME_STATE_SERVICE_PID" >/dev/null 2>&1; then
    if backend_runtime_process_matches_state_before_deadline "$deadline" "$service_name" "$module_name" "$RUNTIME_STATE_SERVICE_PORT" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER" "$RUNTIME_STATE_JAVA_PATH" "$RUNTIME_STATE_JAR_PATH"; then
      echo "Refusing to replace live existing ${service_name} service pid=${RUNTIME_STATE_SERVICE_PID} port=${RUNTIME_STATE_SERVICE_PORT}." >&2
    else
      echo "Refusing unprovable existing ${service_name} service pid=${RUNTIME_STATE_SERVICE_PID}." >&2
    fi
    return 1
  fi
  rm -f "$state_file"
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

backend_runtime_launcher_matches_state_before_deadline() {
  local deadline="$1"
  local service_name="$2"
  local launcher_name="$3"
  local launcher_pid="$4"
  local start_marker="$5"
  kill -0 "$launcher_pid" >/dev/null 2>&1 || return 1
  [[ "$start_marker" != "unavailable" ]] || return 1
  [[ "$(backend_runtime_process_start_marker_before_deadline "$deadline" "$launcher_pid")" == "$start_marker" ]] || return 1
  local process_command
  process_command="$(backend_runtime_capture_bounded_command "$deadline" ps -ww -p "$launcher_pid" -o command=)" || return 1
  [[ "$process_command" == *"${launcher_name}"* ]] || return 1
  [[ -n "$service_name" ]]
}

backend_runtime_child_pids_before_deadline() {
  local parent_pid="$1"
  local deadline="$2"
  command -v pgrep >/dev/null 2>&1 || return 1
  local output_file error_file pgrep_status=0
  output_file="$(mktemp "${TMPDIR:-/tmp}/chengdd-pgrep-output.XXXXXX")"
  error_file="$(mktemp "${TMPDIR:-/tmp}/chengdd-pgrep-error.XXXXXX")"
  backend_runtime_run_bounded_command "$deadline" "$output_file" "$error_file" pgrep -P "$parent_pid" || pgrep_status=$?
  if (( pgrep_status != 0 && pgrep_status != 1 )); then
    rm -f "$output_file" "$error_file"
    return 1
  fi
  local children
  children="$(<"$output_file")"
  rm -f "$output_file" "$error_file"
  local child_pid
  while IFS= read -r child_pid; do
    [[ -n "$child_pid" ]] || continue
    [[ "$child_pid" =~ ^[0-9]+$ ]] || return 1
    printf '%s\n' "$child_pid"
  done <<<"$children"
}

backend_runtime_collect_owned_descendants() {
  local parent_pid="$1"
  local deadline="$2"
  local ancestors="${3:-|${parent_pid}|}"
  local descendant_pids child_pid child_marker
  descendant_pids="$(backend_runtime_collect_descendant_pids "$parent_pid" "$deadline" "$ancestors")" || return 1
  while IFS= read -r child_pid; do
    [[ -n "$child_pid" ]] || continue
    child_marker="$(backend_runtime_process_start_marker_before_deadline "$deadline" "$child_pid")" || return 1
    printf '%s|%s\n' "$child_pid" "$child_marker"
  done <<<"$descendant_pids"
}

backend_runtime_collect_descendant_pids() {
  local parent_pid="$1"
  local deadline="$2"
  local ancestors="${3:-|${parent_pid}|}"
  local children child_pid descendants
  children="$(backend_runtime_child_pids_before_deadline "$parent_pid" "$deadline")" || return 1
  while IFS= read -r child_pid; do
    [[ -n "$child_pid" ]] || continue
    [[ "$ancestors" != *"|${child_pid}|"* ]] || return 1
    descendants="$(backend_runtime_collect_descendant_pids "$child_pid" "$deadline" "${ancestors}${child_pid}|")" || return 1
    [[ -z "$descendants" ]] || printf '%s\n' "$descendants"
    printf '%s\n' "$child_pid"
  done <<<"$children"
}

backend_runtime_signal_exact_process() {
  local process_pid="$1"
  local process_marker="$2"
  local signal_name="$3"
  kill -0 "$process_pid" >/dev/null 2>&1 || return 0
  [[ "$(backend_runtime_process_start_marker "$process_pid")" == "$process_marker" ]] || return 1
  kill "-$signal_name" "$process_pid" >/dev/null 2>&1
}

backend_runtime_signal_exact_process_before_deadline() {
  local deadline="$1"
  local process_pid="$2"
  local process_marker="$3"
  local signal_name="$4"
  kill -0 "$process_pid" >/dev/null 2>&1 || return 0
  [[ "$(backend_runtime_process_start_marker_before_deadline "$deadline" "$process_pid")" == "$process_marker" ]] || return 1
  kill "-$signal_name" "$process_pid" >/dev/null 2>&1
}

backend_runtime_process_is_stopped_before_deadline() {
  local deadline="$1"
  local process_pid="$2"
  local process_state
  if [[ -r "/proc/${process_pid}/stat" ]]; then
    process_state="$(backend_runtime_capture_bounded_command "$deadline" awk '{print $3}' "/proc/${process_pid}/stat")" || return 1
  else
    process_state="$(backend_runtime_capture_bounded_command "$deadline" ps -p "$process_pid" -o state=)" || return 1
    process_state="${process_state//[[:space:]]/}"
  fi
  [[ "$process_state" == T* || "$process_state" == t* ]]
}

backend_runtime_resume_tree_entries() {
  local deadline="$1"
  shift
  local entry process_pid process_marker resume_status=0
  for entry in "$@"; do
    IFS='|' read -r process_pid process_marker <<<"$entry"
    kill -0 "$process_pid" >/dev/null 2>&1 || continue
    backend_runtime_signal_exact_process_before_deadline "$deadline" "$process_pid" "$process_marker" CONT || resume_status=1
  done
  return "$resume_status"
}

backend_runtime_terminate_owned_tree() {
  local root_pid="$1"
  local root_marker="$2"
  local deadline="$3"
  local process_label="$4"
  (( $(date +%s) < deadline )) || return 1
  [[ "$(backend_runtime_process_start_marker_before_deadline "$deadline" "$root_pid")" == "$root_marker" ]] || return 1

  local descendants_output
  descendants_output="$(backend_runtime_collect_owned_descendants "$root_pid" "$deadline")" || {
    echo "Cannot safely inspect descendants for ${process_label}." >&2
    return 1
  }
  local tree_entries=("${root_pid}|${root_marker}")
  local tree_seen="|${root_pid}|"
  local child_pid child_marker entry process_pid process_marker
  while IFS='|' read -r child_pid child_marker; do
    [[ -n "$child_pid" ]] || continue
    [[ "$tree_seen" != *"|${child_pid}|"* ]] || continue
    tree_entries+=("${child_pid}|${child_marker}")
    tree_seen+="${child_pid}|"
  done <<<"$descendants_output"

  # Signal known descendants before the root. No signal occurs until the
  # complete initial tree has been proved.
  local index
  for ((index=1; index<${#tree_entries[@]}; index++)); do
    IFS='|' read -r process_pid process_marker <<<"${tree_entries[$index]}"
    backend_runtime_signal_exact_process_before_deadline "$deadline" "$process_pid" "$process_marker" TERM || return 1
  done
  backend_runtime_signal_exact_process_before_deadline "$deadline" "$root_pid" "$root_marker" TERM || return 1

  local term_deadline=$(( $(date +%s) + 1 ))
  local kill_reserve_deadline=$(( deadline - 3 ))
  (( term_deadline < kill_reserve_deadline )) || term_deadline="$kill_reserve_deadline"
  while (( $(date +%s) < term_deadline )); do
    # TERM handlers can create children. Re-prove and signal every new child.
    if kill -0 "$root_pid" >/dev/null 2>&1; then
      descendants_output="$(backend_runtime_collect_owned_descendants "$root_pid" "$deadline")" || {
        echo "Cannot safely rescan descendants for ${process_label}." >&2
        return 1
      }
      while IFS='|' read -r child_pid child_marker; do
        [[ -n "$child_pid" ]] || continue
        if [[ "$tree_seen" != *"|${child_pid}|"* ]]; then
          tree_entries+=("${child_pid}|${child_marker}")
          tree_seen+="${child_pid}|"
          backend_runtime_signal_exact_process_before_deadline "$deadline" "$child_pid" "$child_marker" TERM || return 1
        fi
      done <<<"$descendants_output"
    fi
    local any_alive=0
    for entry in "${tree_entries[@]}"; do
      IFS='|' read -r process_pid process_marker <<<"$entry"
      kill -0 "$process_pid" >/dev/null 2>&1 && any_alive=1
    done
    [[ "$any_alive" -eq 0 ]] && break
    sleep 0.1
  done

  # Freeze the exact root first, then every known child. This prevents the
  # tree from evolving while fixed-point discovery runs.
  local stopped_entries=()
  if kill -0 "$root_pid" >/dev/null 2>&1; then
    backend_runtime_signal_exact_process_before_deadline "$deadline" "$root_pid" "$root_marker" STOP || return 1
    stopped_entries+=("${root_pid}|${root_marker}")
  fi
  for ((index=1; index<${#tree_entries[@]}; index++)); do
    IFS='|' read -r process_pid process_marker <<<"${tree_entries[$index]}"
    if kill -0 "$process_pid" >/dev/null 2>&1; then
      if ! backend_runtime_signal_exact_process_before_deadline "$deadline" "$process_pid" "$process_marker" STOP; then
        backend_runtime_resume_tree_entries "$deadline" "${stopped_entries[@]}" || true
        return 1
      fi
      stopped_entries+=("${process_pid}|${process_marker}")
    fi
  done
  for entry in "${stopped_entries[@]}"; do
    IFS='|' read -r process_pid process_marker <<<"$entry"
    if ! backend_runtime_process_is_stopped_before_deadline "$deadline" "$process_pid"; then
      backend_runtime_resume_tree_entries "$deadline" "${stopped_entries[@]}" || true
      echo "Cannot prove frozen process tree for ${process_label}." >&2
      return 1
    fi
  done

  local stable_snapshots=0
  while (( stable_snapshots < 2 )); do
    (( $(date +%s) < deadline )) || {
      backend_runtime_resume_tree_entries "$deadline" "${stopped_entries[@]}" || true
      return 1
    }
    local added_child=0
    local snapshot_entries=()
    if kill -0 "$root_pid" >/dev/null 2>&1; then
      snapshot_entries=("${root_pid}|${root_marker}")
    else
      snapshot_entries=("${tree_entries[@]}")
    fi
    for entry in "${snapshot_entries[@]}"; do
      IFS='|' read -r process_pid process_marker <<<"$entry"
      kill -0 "$process_pid" >/dev/null 2>&1 || continue
      descendants_output="$(backend_runtime_collect_descendant_pids "$process_pid" "$deadline")" || {
        backend_runtime_resume_tree_entries "$deadline" "${stopped_entries[@]}" || true
        echo "Cannot prove frozen descendants for ${process_label}." >&2
        return 1
      }
      while IFS= read -r child_pid; do
        [[ -n "$child_pid" ]] || continue
        if [[ "$tree_seen" != *"|${child_pid}|"* ]]; then
          child_marker="$(backend_runtime_process_start_marker_before_deadline "$deadline" "$child_pid")" || {
            backend_runtime_resume_tree_entries "$deadline" "${stopped_entries[@]}" || true
            return 1
          }
          if ! backend_runtime_signal_exact_process_before_deadline "$deadline" "$child_pid" "$child_marker" STOP \
            || ! backend_runtime_process_is_stopped_before_deadline "$deadline" "$child_pid"; then
            backend_runtime_resume_tree_entries "$deadline" "${stopped_entries[@]}" || true
            return 1
          fi
          tree_entries+=("${child_pid}|${child_marker}")
          stopped_entries+=("${child_pid}|${child_marker}")
          tree_seen+="${child_pid}|"
          added_child=1
        fi
      done <<<"$descendants_output"
    done
    if [[ "$added_child" -eq 0 ]]; then
      stable_snapshots=$((stable_snapshots + 1))
    else
      stable_snapshots=0
    fi
  done

  # Every known process is frozen. Kill descendants before the root.
  for ((index=1; index<${#tree_entries[@]}; index++)); do
    IFS='|' read -r process_pid process_marker <<<"${tree_entries[$index]}"
    kill -0 "$process_pid" >/dev/null 2>&1 || continue
    backend_runtime_signal_exact_process_before_deadline "$deadline" "$process_pid" "$process_marker" KILL || return 1
  done
  if kill -0 "$root_pid" >/dev/null 2>&1; then
    backend_runtime_signal_exact_process_before_deadline "$deadline" "$root_pid" "$root_marker" KILL || return 1
  fi

  while (( $(date +%s) < deadline )); do
    local survivors=0
    for entry in "${tree_entries[@]}"; do
      IFS='|' read -r process_pid process_marker <<<"$entry"
      kill -0 "$process_pid" >/dev/null 2>&1 && survivors=1
    done
    [[ "$survivors" -eq 0 ]] && break
    sleep 0.1
  done
  if ! kill -0 "$root_pid" >/dev/null 2>&1; then
    wait "$root_pid" >/dev/null 2>&1 || true
  fi
  for entry in "${tree_entries[@]}"; do
    IFS='|' read -r process_pid process_marker <<<"$entry"
    if kill -0 "$process_pid" >/dev/null 2>&1; then
      echo "Owned process tree survivor for ${process_label}: pid=${process_pid}" >&2
      return 1
    fi
  done
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
  local start_marker="$5"
  local deadline="$6"
  local state_dir
  state_dir="$(backend_runtime_state_dir "$repo_root")"
  mkdir -p "$state_dir/logs"

  if [[ -z "$start_marker" ]] || ! backend_runtime_launcher_matches_state_before_deadline "$deadline" "$service_name" "$launcher_name" "$launcher_pid" "$start_marker"; then
    echo "Unable to record launcher start marker for ${service_name} pid=${launcher_pid}." >&2
    return 1
  fi

  local launcher_state_file temporary_state_file
  launcher_state_file="$(backend_runtime_launcher_state_file "$repo_root" "$service_name")"
  temporary_state_file="$(umask 077; mktemp "${launcher_state_file}.tmp.XXXXXX")" || return 1
  if ! cat >"$temporary_state_file" <<EOF
SERVICE_NAME=${service_name}
LAUNCHER_NAME=${launcher_name}
LAUNCHER_PID=${launcher_pid}
PROCESS_START_MARKER=${start_marker}
EOF
  then
    rm -f "$temporary_state_file"
    return 1
  fi
  if (( $(date +%s) >= deadline )) \
    || ! backend_runtime_launcher_matches_state_before_deadline "$deadline" "$service_name" "$launcher_name" "$launcher_pid" "$start_marker" \
    || ! mv -f "$temporary_state_file" "$launcher_state_file"; then
    rm -f "$temporary_state_file"
    return 1
  fi
}

compute_backend_runtime_fingerprint() {
  local repo_root="$1"
  (
    cd "$repo_root"
    git rev-parse HEAD 2>/dev/null || echo "NO_GIT_HEAD"
    git status --porcelain --untracked-files=no -- cdd-parent config db scripts/local README.md 2>/dev/null || true
  ) | shasum -a 256 | awk '{print $1}'
}

compute_backend_runtime_identity() {
  local repo_root="$1"
  local git_head fingerprint
  git_head="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null || echo "unknown")"
  fingerprint="$(compute_backend_runtime_fingerprint "$repo_root")"
  printf 'GIT_HEAD=%s\nBACKEND_FINGERPRINT=%s\n' "$git_head" "$fingerprint"
}

compute_backend_runtime_identity_before_deadline() {
  local deadline="$1"
  local repo_root="$2"
  local git_head_status=0 git_status_status=0
  local git_head git_changes fingerprint_input hash_output fingerprint
  git_head="$(backend_runtime_capture_bounded_command "$deadline" git -C "$repo_root" rev-parse HEAD)" || git_head_status=$?
  if (( git_head_status == 124 )); then
    return 1
  elif (( git_head_status != 0 )); then
    git_head="unknown"
  fi
  git_changes="$(backend_runtime_capture_bounded_command "$deadline" git -C "$repo_root" status --porcelain --untracked-files=no -- cdd-parent config db scripts/local README.md)" || git_status_status=$?
  (( git_status_status != 124 )) || return 1
  (( git_status_status == 0 )) || git_changes=""

  fingerprint_input="$(mktemp "${TMPDIR:-/tmp}/chengdd-runtime-fingerprint.XXXXXX")"
  if [[ "$git_head" == "unknown" ]]; then
    printf 'NO_GIT_HEAD\n%s\n' "$git_changes" >"$fingerprint_input"
  else
    printf '%s\n%s\n' "$git_head" "$git_changes" >"$fingerprint_input"
  fi
  hash_output="$(backend_runtime_capture_bounded_command "$deadline" shasum -a 256 "$fingerprint_input")" || {
    rm -f "$fingerprint_input"
    return 1
  }
  rm -f "$fingerprint_input"
  fingerprint="${hash_output%%[[:space:]]*}"
  [[ "$fingerprint" =~ ^[0-9a-f]{64}$ ]] || return 1
  printf 'GIT_HEAD=%s\nBACKEND_FINGERPRINT=%s\n' "$git_head" "$fingerprint"
}

record_backend_runtime_state() {
  local repo_root="$1"
  local service_name="$2"
  local module_name="$3"
  local service_port="$4"
  local service_pid="${5:-}"
  local supplied_start_marker="${6:-}"
  local java_path="$7"
  local jar_path="$8"
  local readiness_deadline="$9"
  local state_dir
  state_dir="$(backend_runtime_state_dir "$repo_root")"
  mkdir -p "$state_dir"

  (( $(date +%s) < readiness_deadline )) || return 1
  local identity_output_file
  identity_output_file="$(mktemp "${state_dir}/.runtime-identity.XXXXXX")"
  if ! compute_backend_runtime_identity_before_deadline "$readiness_deadline" "$repo_root" >"$identity_output_file"; then
    rm -f "$identity_output_file"
    echo "Runtime identity inspection failed or exceeded its deadline for ${service_name}." >&2
    return 1
  fi
  local git_head=""
  local fingerprint=""
  local identity_key identity_value
  while IFS='=' read -r identity_key identity_value || [[ -n "$identity_key" ]]; do
    case "$identity_key" in
      GIT_HEAD) git_head="$identity_value" ;;
      BACKEND_FINGERPRINT) fingerprint="$identity_value" ;;
      *)
        rm -f "$identity_output_file"
        return 1
        ;;
    esac
  done < "$identity_output_file"
  rm -f "$identity_output_file"
  [[ -n "$git_head" && "$fingerprint" =~ ^[0-9a-f]{64}$ ]] || return 1
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
  (( $(date +%s) < readiness_deadline )) || return 1
  if ! cat > "$temporary_state_file" <<EOF
SERVICE_NAME=${service_name}
MODULE_NAME=${module_name}
SERVICE_PORT=${service_port}
SERVICE_PID=${service_pid}
PROCESS_START_MARKER=${process_start_marker}
JAVA_PATH=${java_path}
JAR_PATH=${jar_path}
GIT_HEAD=${git_head}
BACKEND_FINGERPRINT=${fingerprint}
STARTED_AT=${started_at}
STARTED_AT_TEXT=${started_at_text}
EOF
  then
    rm -f "$temporary_state_file"
    return 1
  fi
  if ! backend_runtime_process_matches_state_before_deadline "$readiness_deadline" "$service_name" "$module_name" "$service_port" "$service_pid" "$process_start_marker" "$java_path" "$jar_path" \
    || ! backend_runtime_listener_matches_process "$service_port" "$service_pid" "$readiness_deadline"; then
    rm -f "$temporary_state_file"
    echo "Refusing to publish stale or unowned runtime state for ${service_name} pid=${service_pid}." >&2
    return 1
  fi
  if (( $(date +%s) >= readiness_deadline )); then
    rm -f "$temporary_state_file"
    return 1
  fi
  if ! mv -f "$temporary_state_file" "$state_file"; then
    rm -f "$temporary_state_file"
    return 1
  fi
  if (( $(date +%s) >= readiness_deadline )); then
    rm -f "$state_file"
    return 1
  fi
}
