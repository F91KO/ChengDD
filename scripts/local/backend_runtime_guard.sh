#!/usr/bin/env bash
set -euo pipefail

backend_runtime_state_dir() {
  local repo_root="$1"
  echo "$repo_root/.local/backend-runtime"
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
  started_at_text="$(date '+%Y-%m-%d %H:%M:%S')"

  cat > "${state_dir}/${service_name}.env" <<EOF
SERVICE_NAME='${service_name}'
MODULE_NAME='${module_name}'
SERVICE_PORT='${service_port}'
GIT_HEAD='${git_head}'
BACKEND_FINGERPRINT='${fingerprint}'
STARTED_AT='${started_at}'
STARTED_AT_TEXT='${started_at_text}'
EOF
}
