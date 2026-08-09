#!/usr/bin/env bash
set -euo pipefail

runtime_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$runtime_script_dir/backend_runtime_guard.sh"

java_major_version() {
  local java_home="$1"
  local version_line=""

  if [[ ! -x "$java_home/bin/java" ]]; then
    return 1
  fi

  version_line="$("$java_home/bin/java" -version 2>&1 | head -n 1)"
  if [[ "$version_line" =~ version\ \"1\.([0-9]+)\. ]]; then
    echo "${BASH_REMATCH[1]}"
    return 0
  fi
  if [[ "$version_line" =~ version\ \"([0-9]+) ]]; then
    echo "${BASH_REMATCH[1]}"
    return 0
  fi

  return 1
}

resolve_java_home() {
  local required_major="$1"
  local current_major=""
  local detected_java_home=""
  local candidate=""
  local candidates=()

  if [[ -n "${JAVA_HOME:-}" ]]; then
    current_major="$(java_major_version "$JAVA_HOME" || true)"
    if [[ "$current_major" == "$required_major" ]]; then
      export JAVA_HOME
      return 0
    fi
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    detected_java_home="$("/usr/libexec/java_home" -v "$required_major" 2>/dev/null || true)"
    if [[ -n "$detected_java_home" ]]; then
      candidates+=("$detected_java_home")
    fi
  fi

  candidates+=(
    "/usr/local/opt/openjdk@${required_major}/libexec/openjdk.jdk/Contents/Home"
    "/opt/homebrew/opt/openjdk@${required_major}/libexec/openjdk.jdk/Contents/Home"
    "/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
  )

  for candidate in "${candidates[@]}"; do
    current_major="$(java_major_version "$candidate" || true)"
    if [[ "$current_major" == "$required_major" ]]; then
      export JAVA_HOME="$candidate"
      return 0
    fi
  done

  return 1
}

wait_for_service_health() {
  local service_name="$1"
  local service_port="$2"
  local service_pid="$3"
  local health_deadline="$4"
  local health_url="http://127.0.0.1:${service_port}/actuator/health"
  local now remaining connect_timeout request_timeout

  while :; do
    now="$(date +%s)"
    remaining=$(( health_deadline - now ))
    (( remaining > 0 )) || break
    connect_timeout=$(( remaining < 2 ? remaining : 2 ))
    request_timeout=$(( remaining < 5 ? remaining : 5 ))
    if curl --silent --show-error --fail --connect-timeout "$connect_timeout" --max-time "$request_timeout" "$health_url" >/dev/null 2>&1; then
      return 0
    fi
    if ! kill -0 "$service_pid" >/dev/null 2>&1; then
      echo "${service_name} 启动失败，进程已退出。" >&2
      return 1
    fi
    (( $(date +%s) < health_deadline )) && sleep 1
  done

  echo "${service_name} 未在启动截止时间前通过健康检查：${health_url}" >&2
  return 1
}

cleanup_started_service() {
  local service_name="$1"
  local service_pid="$2"
  local start_marker="$3"
  local startup_deadline="$4"

  kill -0 "$service_pid" >/dev/null 2>&1 || {
    wait "$service_pid" >/dev/null 2>&1 || true
    return 0
  }
  if [[ -n "$start_marker" && "$(backend_runtime_process_start_marker "$service_pid")" != "$start_marker" ]]; then
    echo "Refusing cleanup for ${service_name}: pid ${service_pid} no longer has the launched start marker." >&2
    return 1
  fi

  local now="$(date +%s)"
  local remaining=$(( startup_deadline - now ))
  if (( remaining > 1 )); then
    kill -TERM "$service_pid" >/dev/null 2>&1 || true
    local term_deadline=$(( now + 1 ))
    local kill_reserve_deadline=$(( startup_deadline - 1 ))
    (( term_deadline < kill_reserve_deadline )) || term_deadline="$kill_reserve_deadline"
    while kill -0 "$service_pid" >/dev/null 2>&1 && (( $(date +%s) < term_deadline )); do
      [[ -z "$start_marker" || "$(backend_runtime_process_start_marker "$service_pid")" == "$start_marker" ]] || return 1
      sleep 0.1
    done
  fi

  if kill -0 "$service_pid" >/dev/null 2>&1; then
    [[ -z "$start_marker" || "$(backend_runtime_process_start_marker "$service_pid")" == "$start_marker" ]] || return 1
    kill -KILL "$service_pid" >/dev/null 2>&1 || return 1
  fi
  while kill -0 "$service_pid" >/dev/null 2>&1 && (( $(date +%s) < startup_deadline )); do
    sleep 0.1
  done
  if kill -0 "$service_pid" >/dev/null 2>&1; then
    echo "Unable to stop owned ${service_name} pid=${service_pid} before the startup deadline." >&2
    return 1
  fi
  wait "$service_pid" >/dev/null 2>&1 || true
}

fail_started_service() {
  local service_name="$1"
  local service_pid="$2"
  local start_marker="$3"
  local startup_deadline="$4"
  if ! cleanup_started_service "$service_name" "$service_pid" "$start_marker" "$startup_deadline"; then
    echo "Startup failed and owned-process cleanup did not complete for ${service_name} pid=${service_pid}." >&2
    return 2
  fi
  return 1
}

run_packaged_module() {
  local repo_root="$1"
  local parent_root="$2"
  local settings_file="$3"
  local work_repo="$4"
  local module_name="$5"
  local service_name="$6"
  local service_port
  service_port="$(backend_runtime_service_port "$service_name")"
  configure_backend_runtime

  if [[ "$runtime_config_mode" == "nacos" && "${CDD_NACOS_CONFIG_PUBLISHED_FOR:-}" != "$runtime_env" ]]; then
    "$repo_root/scripts/nacos/publish_nacos_configs.sh" "$runtime_env"
    export CDD_NACOS_CONFIG_PUBLISHED_FOR="$runtime_env"
  fi

  mvn -q -s "$settings_file" "-Dmaven.repo.local=$work_repo" -f "${parent_root}/pom.xml" \
    -pl "${module_name}" -am \
    package -DskipTests

  local jar_path="${parent_root}/${module_name}/target/${module_name}-0.1.0-SNAPSHOT.jar"
  local java_path="$JAVA_HOME/bin/java"
  if [[ ! -f "$jar_path" ]]; then
    echo "未找到可执行包：${jar_path}" >&2
    return 1
  fi

  if ! backend_runtime_assert_port_available "$service_port"; then
    echo "Refusing to start ${service_name} on an occupied or unverifiable port ${service_port}." >&2
    return 1
  fi

  local startup_timeout_seconds="${CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS:-60}"
  [[ "$startup_timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
    echo "CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS must be a positive integer." >&2
    return 1
  }
  local cleanup_reserve_seconds="${CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS:-5}"
  [[ "$cleanup_reserve_seconds" =~ ^[1-9][0-9]*$ ]] || {
    echo "CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS must be a positive integer." >&2
    return 1
  }
  if (( cleanup_reserve_seconds >= startup_timeout_seconds )); then
    cleanup_reserve_seconds=$(( startup_timeout_seconds > 1 ? startup_timeout_seconds - 1 : 1 ))
  fi
  local startup_deadline=$(( $(date +%s) + startup_timeout_seconds ))
  local health_deadline=$(( startup_deadline - cleanup_reserve_seconds ))

  "$java_path" -jar "$jar_path" --server.port="${service_port}" --spring.profiles.active="${runtime_env},${runtime_config_mode}" &
  local service_pid=$!
  local process_start_marker
  process_start_marker="$(wait_for_backend_runtime_process_start_marker "$service_pid" || true)"
  if [[ -z "$process_start_marker" ]] || ! backend_runtime_process_matches_state_before_deadline "$startup_deadline" "$service_name" "$module_name" "$service_port" "$service_pid" "$process_start_marker" "$java_path" "$jar_path"; then
    echo "Unable to prove ownership of launched ${service_name} pid=${service_pid}." >&2
    local failure_status=0
    fail_started_service "$service_name" "$service_pid" "$process_start_marker" "$startup_deadline" || failure_status=$?
    return "$failure_status"
  fi

  if ! wait_for_service_health "$service_name" "$service_port" "$service_pid" "$health_deadline"; then
    local failure_status=0
    fail_started_service "$service_name" "$service_pid" "$process_start_marker" "$startup_deadline" || failure_status=$?
    return "$failure_status"
  fi

  if (( $(date +%s) >= health_deadline )) \
    || ! backend_runtime_process_matches_state_before_deadline "$health_deadline" "$service_name" "$module_name" "$service_port" "$service_pid" "$process_start_marker" "$java_path" "$jar_path" \
    || ! backend_runtime_listener_matches_process "$service_port" "$service_pid" "$health_deadline"; then
    echo "Healthy endpoint for ${service_name} is not owned by the exact launched JVM pid=${service_pid}." >&2
    local failure_status=0
    fail_started_service "$service_name" "$service_pid" "$process_start_marker" "$startup_deadline" || failure_status=$?
    return "$failure_status"
  fi

  if ! record_backend_runtime_state "$repo_root" "$service_name" "$module_name" "$service_port" "$service_pid" "$process_start_marker" "$java_path" "$jar_path" "$health_deadline"; then
    local failure_status=0
    fail_started_service "$service_name" "$service_pid" "$process_start_marker" "$startup_deadline" || failure_status=$?
    return "$failure_status"
  fi
  wait "$service_pid"
}
