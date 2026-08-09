#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

stop_timeout_seconds="${CDD_RUNTIME_STOP_TIMEOUT_SECONDS:-30}"
term_grace_seconds="${CDD_RUNTIME_TERM_GRACE_SECONDS:-5}"
[[ "$stop_timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_RUNTIME_STOP_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}
[[ "$term_grace_seconds" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_RUNTIME_TERM_GRACE_SECONDS must be a positive integer." >&2
  exit 1
}
runtime_catalog="$(backend_runtime_service_catalog)"
local_stop_deadline=$(( $(date +%s) + stop_timeout_seconds ))

owned_java_pid=""
owned_java_marker=""
owned_java_port=""
owned_launcher_pid=""
owned_launcher_marker=""

load_owned_java() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local inspection_deadline="${4:-$local_stop_deadline}"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  owned_java_pid=""
  owned_java_marker=""
  owned_java_port=""
  reset_backend_runtime_state_globals

  [[ -f "$state_file" ]] || return 1
  if ! read_backend_runtime_state "$state_file"; then
    echo "Refusing malformed runtime state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  local expected_jar_path="$repo_root/cdd-parent/${module_name}/target/${module_name}-0.1.0-SNAPSHOT.jar"
  if [[ "$RUNTIME_STATE_SERVICE_NAME" != "$service_name" || "$RUNTIME_STATE_MODULE_NAME" != "$module_name" || "$RUNTIME_STATE_JAR_PATH" != "$expected_jar_path" ]]; then
    echo "Refusing mismatched runtime state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if ! kill -0 "$RUNTIME_STATE_SERVICE_PID" >/dev/null 2>&1; then
    return 1
  fi
  if ! backend_runtime_process_matches_state_before_deadline "$inspection_deadline" "$service_name" "$module_name" "$RUNTIME_STATE_SERVICE_PORT" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER" "$RUNTIME_STATE_JAVA_PATH" "$RUNTIME_STATE_JAR_PATH"; then
    echo "Refusing unowned runtime process for ${service_name} pid=${RUNTIME_STATE_SERVICE_PID}." >&2
    return 2
  fi
  owned_java_pid="$RUNTIME_STATE_SERVICE_PID"
  owned_java_marker="$RUNTIME_STATE_PROCESS_START_MARKER"
  owned_java_port="$RUNTIME_STATE_SERVICE_PORT"
  return 0
}

load_owned_launcher() {
  local service_name="$1"
  local launcher_name="$2"
  local inspection_deadline="${3:-$local_stop_deadline}"
  local state_file
  state_file="$(backend_runtime_launcher_state_file "$repo_root" "$service_name")"
  owned_launcher_pid=""
  owned_launcher_marker=""
  reset_backend_runtime_launcher_state_globals

  [[ -f "$state_file" ]] || return 1
  if ! read_backend_runtime_launcher_state "$state_file"; then
    echo "Refusing malformed launcher state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if [[ "$RUNTIME_LAUNCHER_SERVICE_NAME" != "$service_name" || "$RUNTIME_LAUNCHER_NAME" != "$launcher_name" ]]; then
    echo "Refusing mismatched launcher state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if ! kill -0 "$RUNTIME_LAUNCHER_PID" >/dev/null 2>&1; then
    return 1
  fi
  if ! backend_runtime_launcher_matches_state_before_deadline "$inspection_deadline" "$service_name" "$launcher_name" "$RUNTIME_LAUNCHER_PID" "$RUNTIME_LAUNCHER_START_MARKER"; then
    echo "Refusing unowned launcher process for ${service_name} pid=${RUNTIME_LAUNCHER_PID}." >&2
    return 2
  fi
  owned_launcher_pid="$RUNTIME_LAUNCHER_PID"
  owned_launcher_marker="$RUNTIME_LAUNCHER_START_MARKER"
  return 0
}

find_unmanaged_service_process() {
  local module_name="$1"
  local service_port="$2"
  local inspection_deadline="${3:-$local_stop_deadline}"
  local process_list="${RUNTIME_UNMANAGED_PROCESS_SNAPSHOT:-}"
  if [[ "${RUNTIME_UNMANAGED_PROCESS_SNAPSHOT_READY:-0}" -ne 1 ]]; then
    command -v ps >/dev/null 2>&1 || return 2
    process_list="$(backend_runtime_capture_bounded_command "$inspection_deadline" ps -ax -o pid= -o command=)" || return 2
  fi
  local process_line process_arguments=() argument jar_found port_found
  while IFS= read -r process_line; do
    [[ -n "$process_line" ]] || continue
    process_arguments=()
    read -r -a process_arguments <<<"$process_line"
    jar_found=0
    port_found=0
    for argument in "${process_arguments[@]}"; do
      [[ "$argument" == *"/${module_name}-0.1.0-SNAPSHOT.jar" ]] && jar_found=1
      [[ "$argument" == "--server.port=${service_port}" ]] && port_found=1
    done
    [[ "$jar_found" -eq 1 && "$port_found" -eq 1 ]] && return 0
  done <<<"$process_list"
  return 1
}

service_is_confirmed_stopped() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local launcher_name="$4"
  local state_result=0
  local recorded_port="$service_port"

  load_owned_java "$service_name" "$module_name" "$service_port" || state_result=$?
  if [[ "$state_result" -eq 0 ]]; then
    return 1
  fi
  if [[ -n "${RUNTIME_STATE_SERVICE_PORT:-}" ]]; then
    recorded_port="$RUNTIME_STATE_SERVICE_PORT"
  fi
  [[ "$state_result" -eq 1 ]] || return 2

  state_result=0
  load_owned_launcher "$service_name" "$launcher_name" || state_result=$?
  if [[ "$state_result" -eq 0 ]]; then
    return 1
  fi
  [[ "$state_result" -eq 1 ]] || return 2

  local unmanaged_result=0
  find_unmanaged_service_process "$module_name" "$recorded_port" "$local_stop_deadline" || unmanaged_result=$?
  if [[ "$unmanaged_result" -eq 0 ]]; then
    echo "Unmanaged matching process remains for ${service_name}; refusing termination." >&2
    return 2
  fi
  [[ "$unmanaged_result" -eq 1 ]] && return 0
  return 2
}

catalog_entries=()
while IFS= read -r entry; do
  catalog_entries+=("$entry")
done <<<"$runtime_catalog"

shutdown_target_pids=()
shutdown_target_markers=()
shutdown_target_labels=()
shutdown_target_groups=()
shutdown_target_services=()
shutdown_target_is_root=()
shutdown_target_seen='|'
shutdown_launcher_root_pids=()
shutdown_launcher_root_markers=()
shutdown_launcher_root_groups=()
shutdown_launcher_root_services=()
shutdown_unsafe_groups='|'
shutdown_preserved_services='|'
shutdown_safety_problem=0

add_shutdown_target() {
  local target_pid="$1"
  local target_marker="$2"
  local target_label="$3"
  local target_group="$4"
  local target_service="$5"
  local target_is_root="$6"
  if [[ "$shutdown_target_seen" == *"|${target_pid}|"* ]]; then
    local existing_index
    for ((existing_index=0; existing_index<${#shutdown_target_pids[@]}; existing_index++)); do
      if [[ "${shutdown_target_pids[$existing_index]}" == "$target_pid" ]]; then
        [[ "${shutdown_target_markers[$existing_index]}" == "$target_marker" ]] || return 1
        if [[ "${shutdown_target_groups[$existing_index]}" != "$target_group" ]]; then
          if [[ "$target_group" == launcher:* ]]; then
            shutdown_target_groups[$existing_index]="$target_group"
            shutdown_target_services[$existing_index]="$target_service"
          elif [[ "${shutdown_target_groups[$existing_index]}" != launcher:* ]]; then
            return 1
          fi
        fi
        [[ "$target_is_root" -eq 0 ]] || shutdown_target_is_root[$existing_index]=1
        return 0
      fi
    done
    return 1
  fi
  shutdown_target_seen+="${target_pid}|"
  shutdown_target_pids+=("$target_pid")
  shutdown_target_markers+=("$target_marker")
  shutdown_target_labels+=("$target_label")
  shutdown_target_groups+=("$target_group")
  shutdown_target_services+=("$target_service")
  shutdown_target_is_root+=("$target_is_root")
}

mark_shutdown_group_unsafe() {
  local target_group="$1"
  local target_service="$2"
  shutdown_safety_problem=1
  [[ "$shutdown_unsafe_groups" == *"|${target_group}|"* ]] || shutdown_unsafe_groups+="${target_group}|"
  [[ "$shutdown_preserved_services" == *"|${target_service}|"* ]] || shutdown_preserved_services+="${target_service}|"
}

shutdown_group_is_safe() {
  local target_group="$1"
  [[ "$shutdown_unsafe_groups" != *"|${target_group}|"* ]]
}

resume_unsafe_shutdown_targets() {
  local target_index target_pid target_marker target_group
  [[ ${#shutdown_target_pids[@]} -gt 0 ]] || return 0
  for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
    target_group="${shutdown_target_groups[$target_index]}"
    shutdown_group_is_safe "$target_group" && continue
    target_pid="${shutdown_target_pids[$target_index]}"
    target_marker="${shutdown_target_markers[$target_index]}"
    kill -0 "$target_pid" >/dev/null 2>&1 || continue
    backend_runtime_signal_exact_process_before_deadline "$local_stop_deadline" "$target_pid" "$target_marker" CONT || true
  done
}

for ((index=${#catalog_entries[@]} - 1; index >= 0; index--)); do
  IFS='|' read -r service_name module_name service_port launcher_name <<<"${catalog_entries[$index]}"
  java_load_result=0
  load_owned_java "$service_name" "$module_name" "$service_port" "$local_stop_deadline" || java_load_result=$?
  service_java_pid="$owned_java_pid"
  service_java_marker="$owned_java_marker"

  launcher_load_result=0
  load_owned_launcher "$service_name" "$launcher_name" "$local_stop_deadline" || launcher_load_result=$?
  service_launcher_pid="$owned_launcher_pid"
  service_launcher_marker="$owned_launcher_marker"

  if [[ "$java_load_result" -eq 2 || "$launcher_load_result" -eq 2 ]]; then
    mark_shutdown_group_unsafe "service:${service_name}" "$service_name"
    continue
  fi

  if [[ "$launcher_load_result" -eq 0 ]]; then
    descendants_output="$(backend_runtime_collect_owned_descendants "$service_launcher_pid" "$local_stop_deadline")" || {
      echo "Cannot safely snapshot descendants for ${service_name} launcher." >&2
      mark_shutdown_group_unsafe "launcher:${service_launcher_pid}" "$service_name"
      continue
    }
    if [[ "$java_load_result" -eq 0 && "$service_java_pid" != "$service_launcher_pid" ]]; then
      service_java_is_descendant=0
      while IFS='|' read -r descendant_pid descendant_marker; do
        if [[ "$descendant_pid" == "$service_java_pid" && "$descendant_marker" == "$service_java_marker" ]]; then
          service_java_is_descendant=1
          break
        fi
      done <<<"$descendants_output"
      if [[ "$service_java_is_descendant" -ne 1 ]]; then
        echo "Service PID is not a proved descendant of ${service_name} launcher." >&2
        mark_shutdown_group_unsafe "launcher:${service_launcher_pid}" "$service_name"
        continue
      fi
    fi
    launcher_group="launcher:${service_launcher_pid}"
    while IFS='|' read -r descendant_pid descendant_marker; do
      [[ -n "$descendant_pid" ]] || continue
      add_shutdown_target "$descendant_pid" "$descendant_marker" "$service_name launcher child" "$launcher_group" "$service_name" 0 || mark_shutdown_group_unsafe "$launcher_group" "$service_name"
    done <<<"$descendants_output"
    add_shutdown_target "$service_launcher_pid" "$service_launcher_marker" "$service_name launcher" "$launcher_group" "$service_name" 1 || mark_shutdown_group_unsafe "$launcher_group" "$service_name"
    shutdown_launcher_root_pids+=("$service_launcher_pid")
    shutdown_launcher_root_markers+=("$service_launcher_marker")
    shutdown_launcher_root_groups+=("$launcher_group")
    shutdown_launcher_root_services+=("$service_name")
  elif [[ "$java_load_result" -eq 0 ]]; then
    java_group="java:${service_java_pid}"
    add_shutdown_target "$service_java_pid" "$service_java_marker" "$service_name service" "$java_group" "$service_name" 1 || mark_shutdown_group_unsafe "$java_group" "$service_name"
  fi
done

# Phase 1: signal every proven target promptly in reverse service order.
if [[ ${#shutdown_target_pids[@]} -gt 0 ]]; then
  for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
    target_group="${shutdown_target_groups[$target_index]}"
    shutdown_group_is_safe "$target_group" || continue
    target_pid="${shutdown_target_pids[$target_index]}"
    target_marker="${shutdown_target_markers[$target_index]}"
    if ! backend_runtime_signal_exact_process_before_deadline "$local_stop_deadline" "$target_pid" "$target_marker" TERM; then
      mark_shutdown_group_unsafe "$target_group" "${shutdown_target_services[$target_index]}"
    fi
  done

  # Phase 2: one shared TERM grace. Re-scan every live proved launcher root
  # so TERM-created descendants are added and signalled promptly.
  term_deadline=$(( $(date +%s) + term_grace_seconds ))
  kill_reserve_deadline=$(( local_stop_deadline - 1 ))
  (( term_deadline < kill_reserve_deadline )) || term_deadline="$kill_reserve_deadline"
  while (( $(date +%s) < term_deadline )); do
    if [[ ${#shutdown_launcher_root_pids[@]} -gt 0 ]]; then
      for ((root_index=0; root_index<${#shutdown_launcher_root_pids[@]}; root_index++)); do
        root_pid="${shutdown_launcher_root_pids[$root_index]}"
        root_marker="${shutdown_launcher_root_markers[$root_index]}"
        root_group="${shutdown_launcher_root_groups[$root_index]}"
        root_service="${shutdown_launcher_root_services[$root_index]}"
        shutdown_group_is_safe "$root_group" || continue
        if kill -0 "$root_pid" >/dev/null 2>&1; then
          descendants_output="$(backend_runtime_collect_owned_descendants "$root_pid" "$local_stop_deadline")" || {
            mark_shutdown_group_unsafe "$root_group" "$root_service"
            continue
          }
          while IFS='|' read -r descendant_pid descendant_marker; do
            [[ -n "$descendant_pid" ]] || continue
            if [[ "$shutdown_target_seen" != *"|${descendant_pid}|"* ]]; then
              add_shutdown_target "$descendant_pid" "$descendant_marker" "$root_service launcher child" "$root_group" "$root_service" 0 || {
                mark_shutdown_group_unsafe "$root_group" "$root_service"
                continue
              }
              backend_runtime_signal_exact_process_before_deadline "$local_stop_deadline" "$descendant_pid" "$descendant_marker" TERM || mark_shutdown_group_unsafe "$root_group" "$root_service"
            fi
          done <<<"$descendants_output"
        fi
      done
    fi
    any_alive=0
    for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
      shutdown_group_is_safe "${shutdown_target_groups[$target_index]}" || continue
      kill -0 "${shutdown_target_pids[$target_index]}" >/dev/null 2>&1 && any_alive=1
    done
    [[ "$any_alive" -eq 0 ]] && break
    sleep 0.05
  done

  # Phase 3: freeze every safe root and known descendant before fixed-point
  # discovery. Any group that cannot be proved frozen is resumed and retained.
  for desired_root_flag in 1 0; do
    for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
      [[ "${shutdown_target_is_root[$target_index]}" -eq "$desired_root_flag" ]] || continue
      target_group="${shutdown_target_groups[$target_index]}"
      shutdown_group_is_safe "$target_group" || continue
      target_pid="${shutdown_target_pids[$target_index]}"
      target_marker="${shutdown_target_markers[$target_index]}"
      kill -0 "$target_pid" >/dev/null 2>&1 || continue
      if ! backend_runtime_signal_exact_process_before_deadline "$local_stop_deadline" "$target_pid" "$target_marker" STOP; then
        mark_shutdown_group_unsafe "$target_group" "${shutdown_target_services[$target_index]}"
      fi
    done
  done
  for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
    target_group="${shutdown_target_groups[$target_index]}"
    shutdown_group_is_safe "$target_group" || continue
    target_pid="${shutdown_target_pids[$target_index]}"
    kill -0 "$target_pid" >/dev/null 2>&1 || continue
    backend_runtime_process_is_stopped_before_deadline "$local_stop_deadline" "$target_pid" || mark_shutdown_group_unsafe "$target_group" "${shutdown_target_services[$target_index]}"
  done
  resume_unsafe_shutdown_targets

  stable_snapshots=0
  while (( stable_snapshots < 2 && $(date +%s) < local_stop_deadline )); do
    added_descendant=0
    if [[ ${#shutdown_launcher_root_pids[@]} -gt 0 ]]; then
      for ((root_index=0; root_index<${#shutdown_launcher_root_pids[@]}; root_index++)); do
        root_pid="${shutdown_launcher_root_pids[$root_index]}"
        root_marker="${shutdown_launcher_root_markers[$root_index]}"
        root_group="${shutdown_launcher_root_groups[$root_index]}"
        root_service="${shutdown_launcher_root_services[$root_index]}"
        shutdown_group_is_safe "$root_group" || continue
        if ! kill -0 "$root_pid" >/dev/null 2>&1; then
          group_survivor=0
          for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
            [[ "${shutdown_target_groups[$target_index]}" == "$root_group" ]] || continue
            kill -0 "${shutdown_target_pids[$target_index]}" >/dev/null 2>&1 && group_survivor=1
          done
          [[ "$group_survivor" -eq 0 ]] || mark_shutdown_group_unsafe "$root_group" "$root_service"
          continue
        fi
        descendant_pids="$(backend_runtime_collect_descendant_pids "$root_pid" "$local_stop_deadline")" || {
          mark_shutdown_group_unsafe "$root_group" "$root_service"
          continue
        }
        while IFS= read -r descendant_pid; do
          [[ -n "$descendant_pid" ]] || continue
          if [[ "$shutdown_target_seen" != *"|${descendant_pid}|"* ]]; then
            descendant_marker="$(backend_runtime_process_start_marker_before_deadline "$local_stop_deadline" "$descendant_pid")" || {
              mark_shutdown_group_unsafe "$root_group" "$root_service"
              continue
            }
            add_shutdown_target "$descendant_pid" "$descendant_marker" "$root_service launcher child" "$root_group" "$root_service" 0 || {
              mark_shutdown_group_unsafe "$root_group" "$root_service"
              continue
            }
            if ! backend_runtime_signal_exact_process_before_deadline "$local_stop_deadline" "$descendant_pid" "$descendant_marker" STOP \
              || ! backend_runtime_process_is_stopped_before_deadline "$local_stop_deadline" "$descendant_pid"; then
              mark_shutdown_group_unsafe "$root_group" "$root_service"
              continue
            fi
            added_descendant=1
          fi
        done <<<"$descendant_pids"
      done
    fi
    resume_unsafe_shutdown_targets
    if [[ "$added_descendant" -eq 0 ]]; then
      stable_snapshots=$((stable_snapshots + 1))
    else
      stable_snapshots=0
    fi
  done
  if (( stable_snapshots < 2 )); then
    for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
      mark_shutdown_group_unsafe "${shutdown_target_groups[$target_index]}" "${shutdown_target_services[$target_index]}"
    done
    resume_unsafe_shutdown_targets
  fi

  # Every remaining safe group is frozen at a proved fixed point. Kill all
  # descendants first, then roots, under the original absolute deadline.
  for desired_root_flag in 0 1; do
    for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
      [[ "${shutdown_target_is_root[$target_index]}" -eq "$desired_root_flag" ]] || continue
      target_group="${shutdown_target_groups[$target_index]}"
      shutdown_group_is_safe "$target_group" || continue
      target_pid="${shutdown_target_pids[$target_index]}"
      target_marker="${shutdown_target_markers[$target_index]}"
      kill -0 "$target_pid" >/dev/null 2>&1 || continue
      backend_runtime_signal_exact_process_before_deadline "$local_stop_deadline" "$target_pid" "$target_marker" KILL || mark_shutdown_group_unsafe "$target_group" "${shutdown_target_services[$target_index]}"
    done
  done
  while (( $(date +%s) < local_stop_deadline )); do
    survivors=0
    for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
      shutdown_group_is_safe "${shutdown_target_groups[$target_index]}" || continue
      kill -0 "${shutdown_target_pids[$target_index]}" >/dev/null 2>&1 && survivors=1
    done
    [[ "$survivors" -eq 0 ]] && break
    sleep 0.05
  done
  for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
    target_group="${shutdown_target_groups[$target_index]}"
    shutdown_group_is_safe "$target_group" || continue
    if kill -0 "${shutdown_target_pids[$target_index]}" >/dev/null 2>&1; then
      mark_shutdown_group_unsafe "$target_group" "${shutdown_target_services[$target_index]}"
    elif [[ "${shutdown_target_is_root[$target_index]}" -eq 1 ]]; then
      wait "${shutdown_target_pids[$target_index]}" >/dev/null 2>&1 || true
    fi
  done
fi

services_stopped=0
while :; do
  services_stopped=1
  RUNTIME_UNMANAGED_PROCESS_SNAPSHOT_READY=0
  RUNTIME_UNMANAGED_PROCESS_SNAPSHOT="$(backend_runtime_capture_bounded_command "$local_stop_deadline" ps -ax -o pid= -o command=)" \
    && RUNTIME_UNMANAGED_PROCESS_SNAPSHOT_READY=1 \
    || RUNTIME_UNMANAGED_PROCESS_SNAPSHOT=""
  while IFS='|' read -r service_name module_name service_port launcher_name; do
    if [[ "$shutdown_preserved_services" == *"|${service_name}|"* ]]; then
      services_stopped=0
    elif ! service_is_confirmed_stopped "$service_name" "$module_name" "$service_port" "$launcher_name"; then
      services_stopped=0
    else
      remove_backend_runtime_state "$repo_root" "$service_name"
    fi
  done <<<"$runtime_catalog"
  [[ "$services_stopped" -eq 1 ]] && break
  (( $(date +%s) >= local_stop_deadline )) && break
  sleep 1
done

if [[ "$services_stopped" -ne 1 || "$shutdown_safety_problem" -ne 0 ]]; then
  echo "Timed out waiting for owned backend processes to stop safely." >&2
fi

nacos_stopped=1
if [[ "$runtime_config_mode" == "nacos" ]]; then
  nacos_stopped=0
  nacos_stop_deadline=$(( $(date +%s) + stop_timeout_seconds ))
  nacos_checker_script="${CDD_RUNTIME_NACOS_CHECK_SCRIPT:-$repo_root/scripts/nacos/check_nacos_state.sh}"
  while (( $(date +%s) < nacos_stop_deadline )); do
    if CDD_NACOS_DEADLINE_EPOCH="$nacos_stop_deadline" bash "$nacos_checker_script" "$runtime_env" stopped; then
      nacos_stopped=1
      break
    fi
    sleep 1
  done
  if [[ "$nacos_stopped" -ne 1 ]]; then
    echo "Timed out waiting for Nacos instances to deregister." >&2
  fi
fi

if [[ "$services_stopped" -ne 1 || "$shutdown_safety_problem" -ne 0 || "$nacos_stopped" -ne 1 ]]; then
  exit 1
fi

echo "All backend services are stopped."
