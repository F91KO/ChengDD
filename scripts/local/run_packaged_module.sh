#!/usr/bin/env bash
set -euo pipefail

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
  local health_url="http://127.0.0.1:${service_port}/actuator/health"

  for _ in $(seq 1 60); do
    if curl -fsS "$health_url" >/dev/null 2>&1; then
      return 0
    fi
    if ! kill -0 "$service_pid" >/dev/null 2>&1; then
      echo "${service_name} 启动失败，进程已退出。" >&2
      return 1
    fi
    sleep 1
  done

  echo "${service_name} 在 60 秒内未通过健康检查：${health_url}" >&2
  return 1
}

run_packaged_module() {
  local repo_root="$1"
  local parent_root="$2"
  local settings_file="$3"
  local work_repo="$4"
  local module_name="$5"
  local service_name="$6"
  local service_port="$7"

  source "$repo_root/scripts/local/backend_runtime_guard.sh"

  mvn -q -s "$settings_file" "-Dmaven.repo.local=$work_repo" -f "${parent_root}/pom.xml" \
    -pl "${module_name}" -am \
    package -DskipTests

  local jar_path="${parent_root}/${module_name}/target/${module_name}-0.1.0-SNAPSHOT.jar"
  if [[ ! -f "$jar_path" ]]; then
    echo "未找到可执行包：${jar_path}" >&2
    return 1
  fi

  "$JAVA_HOME/bin/java" -jar "$jar_path" --server.port="${service_port}" &
  local service_pid=$!

  if ! wait_for_service_health "$service_name" "$service_port" "$service_pid"; then
    wait "$service_pid"
    return 1
  fi

  record_backend_runtime_state "$repo_root" "$service_name" "$module_name" "$service_port"
  wait "$service_pid"
}
