#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
parent_root="$repo_root/cdd-parent"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17)}"
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "未设置 JAVA_HOME，且未自动发现 JDK 17。" >&2
  exit 1
fi

work_repo="${CDD_MAVEN_REPO:-$repo_root/.m2/repository}"
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
  settings_file="$(mktemp /tmp/chengdd-auth-mvn-settings.XXXXXX)"
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

export CDD_AUTH_DB_URL="${CDD_AUTH_DB_URL:-jdbc:mysql://127.0.0.1:${mysql_port}/${mysql_database}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false}"
export CDD_AUTH_DB_USERNAME="${CDD_AUTH_DB_USERNAME:-$mysql_username}"
export CDD_AUTH_DB_PASSWORD="${CDD_AUTH_DB_PASSWORD:-$mysql_password}"
export CDD_AUTH_DB_DRIVER_CLASS_NAME="${CDD_AUTH_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"
export CDD_AUTH_SQL_INIT_MODE="${CDD_AUTH_SQL_INIT_MODE:-never}"

mvn -q -s "$settings_file" "-Dmaven.repo.local=$work_repo" -f "${parent_root}/cdd-auth-service/pom.xml" \
  spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${CDD_AUTH_SERVER_PORT:-8081}"
