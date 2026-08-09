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

backend_runtime_state_dir() {
  local repo_root="$1"
  echo "$repo_root/.local/backend-runtime"
}

backend_runtime_state_file() {
  local repo_root="$1"
  local service_name="$2"
  printf '%s/%s.env\n' "$(backend_runtime_state_dir "$repo_root")" "$service_name"
}

remove_backend_runtime_state() {
  local repo_root="$1"
  local service_name="$2"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  rm -f "$state_file"
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
  started_at_text="$(date '+%Y-%m-%d %H:%M:%S')"

  cat > "${state_dir}/${service_name}.env" <<EOF
SERVICE_NAME='${service_name}'
MODULE_NAME='${module_name}'
SERVICE_PORT='${service_port}'
SERVICE_PID='${service_pid}'
GIT_HEAD='${git_head}'
BACKEND_FINGERPRINT='${fingerprint}'
STARTED_AT='${started_at}'
STARTED_AT_TEXT='${started_at_text}'
EOF
}
