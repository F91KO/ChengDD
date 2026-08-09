#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

state_dir="$(backend_runtime_state_dir "$repo_root")"
current_fingerprint="$(compute_backend_runtime_fingerprint "$repo_root")"

if [[ ! -d "$state_dir" ]]; then
  echo "未发现后端服务启动记录。请先通过 scripts/local 下的启动脚本启动服务。"
  exit 0
fi

shopt -s nullglob
state_files=("$state_dir"/*.env)
shopt -u nullglob

if [[ ${#state_files[@]} -eq 0 ]]; then
  echo "未发现后端服务启动记录。请先通过 scripts/local 下的启动脚本启动服务。"
  exit 0
fi

has_stale=0
for state_file in "${state_files[@]}"; do
  if ! read_backend_runtime_state "$state_file"; then
    echo "启动记录格式无效: ${state_file}" >&2
    has_stale=1
    continue
  fi
  if [[ "$RUNTIME_STATE_BACKEND_FINGERPRINT" == "$current_fingerprint" ]]; then
    echo "已同步: ${RUNTIME_STATE_SERVICE_NAME} (${RUNTIME_STATE_MODULE_NAME}) 端口 ${RUNTIME_STATE_SERVICE_PORT}，启动时间 ${RUNTIME_STATE_STARTED_AT_TEXT}"
    continue
  fi

  has_stale=1
  echo "需要重启: ${RUNTIME_STATE_SERVICE_NAME} (${RUNTIME_STATE_MODULE_NAME}) 端口 ${RUNTIME_STATE_SERVICE_PORT}，启动时间 ${RUNTIME_STATE_STARTED_AT_TEXT}"
done

if [[ "$has_stale" -eq 1 ]]; then
  echo "检测到后端代码或配置已变化，部分服务与当前工作区不一致。请重启上述服务。"
  exit 1
fi

echo "当前已记录的后端服务均与工作区代码保持一致。"
