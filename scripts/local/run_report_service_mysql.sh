#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
parent_root="$repo_root/cdd-parent"
source "$repo_root/scripts/local/run_packaged_module.sh"

if ! resolve_java_home 21; then
  echo "未设置 JAVA_HOME，且未自动发现 JDK 21。" >&2
  exit 1
fi

work_repo="${CDD_MAVEN_REPO:-$HOME/.m2/repository}"
settings_file="${CDD_MAVEN_SETTINGS:-}"
cleanup_files=()

cleanup() {
  local code=$?
  for file in "${cleanup_files[@]:-}"; do
    [[ -n "$file" ]] || continue
    rm -f "$file"
  done
  exit "$code"
}
trap cleanup EXIT

if [[ -z "$settings_file" ]]; then
  settings_file="$(mktemp /tmp/chengdd-report-mvn-settings.XXXXXX)"
  cleanup_files+=("$settings_file")
  cat >"$settings_file" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
</settings>
EOF
fi

mysql_port="${CDD_LOCAL_MYSQL_PORT:-3306}"
mysql_database="${CDD_LOCAL_MYSQL_DATABASE:-chengdd}"
mysql_username="${CDD_LOCAL_MYSQL_USERNAME:-root}"
mysql_password="${CDD_LOCAL_MYSQL_ROOT_PASSWORD:-change_me}"

export CDD_REPORT_DB_URL="${CDD_REPORT_DB_URL:-jdbc:mysql://127.0.0.1:${mysql_port}/${mysql_database}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false}"
export CDD_REPORT_DB_USERNAME="${CDD_REPORT_DB_USERNAME:-$mysql_username}"
export CDD_REPORT_DB_PASSWORD="${CDD_REPORT_DB_PASSWORD:-$mysql_password}"
export CDD_REPORT_DB_DRIVER_CLASS_NAME="${CDD_REPORT_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"
export CDD_REPORT_SQL_INIT_MODE="${CDD_REPORT_SQL_INIT_MODE:-never}"
report_port="${CDD_REPORT_SERVER_PORT:-8088}"

run_packaged_module "$repo_root" "$parent_root" "$settings_file" "$work_repo" "cdd-report-service" "report-service" "$report_port"
