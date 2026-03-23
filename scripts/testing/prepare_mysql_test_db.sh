#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${BASH_SOURCE[0]:-}" ]]; then
  script_path="${BASH_SOURCE[0]}"
elif [[ -n "${ZSH_VERSION:-}" ]]; then
  script_path="${(%):-%x}"
else
  script_path="$0"
fi

repo_root="$(cd "$(dirname "$script_path")/../.." && pwd)"

container_name="${CDD_LOCAL_MYSQL_CONTAINER_NAME:-cdd-local-mysql}"
mysql_port="${CDD_LOCAL_MYSQL_PORT:-3306}"
mysql_root_password="${CDD_LOCAL_MYSQL_ROOT_PASSWORD:-change_me}"
test_database="${CDD_TEST_MYSQL_DATABASE:-chengdd_test}"
test_username="${CDD_TEST_MYSQL_USERNAME:-root}"
test_password="${CDD_TEST_MYSQL_PASSWORD:-$mysql_root_password}"

if ! docker ps --format '{{.Names}}' | grep -qx "$container_name"; then
  echo "未检测到本地 MySQL 容器 $container_name，请先执行 bash scripts/local/up_local_infra.sh" >&2
  exit 1
fi

docker exec "$container_name" mysql -uroot "-p${mysql_root_password}" -e \
  "DROP DATABASE IF EXISTS \`${test_database}\`; CREATE DATABASE \`${test_database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

export CDD_DB_MIGRATION_URL="jdbc:mysql://127.0.0.1:${mysql_port}/${test_database}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
export CDD_DB_MIGRATION_USERNAME="$test_username"
export CDD_DB_MIGRATION_PASSWORD="$test_password"
export CDD_LOCAL_MYSQL_PORT="$mysql_port"
export CDD_LOCAL_MYSQL_DATABASE="$test_database"

export CDD_AUTH_DB_URL="${CDD_AUTH_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_AUTH_DB_USERNAME="${CDD_AUTH_DB_USERNAME:-$test_username}"
export CDD_AUTH_DB_PASSWORD="${CDD_AUTH_DB_PASSWORD:-$test_password}"
export CDD_AUTH_DB_DRIVER_CLASS_NAME="${CDD_AUTH_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_ORDER_DB_URL="${CDD_ORDER_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_ORDER_DB_USERNAME="${CDD_ORDER_DB_USERNAME:-$test_username}"
export CDD_ORDER_DB_PASSWORD="${CDD_ORDER_DB_PASSWORD:-$test_password}"
export CDD_ORDER_DB_DRIVER_CLASS_NAME="${CDD_ORDER_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_CONFIG_DB_URL="${CDD_CONFIG_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_CONFIG_DB_USERNAME="${CDD_CONFIG_DB_USERNAME:-$test_username}"
export CDD_CONFIG_DB_PASSWORD="${CDD_CONFIG_DB_PASSWORD:-$test_password}"
export CDD_CONFIG_DB_DRIVER_CLASS_NAME="${CDD_CONFIG_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_MERCHANT_DB_URL="${CDD_MERCHANT_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_MERCHANT_DB_USERNAME="${CDD_MERCHANT_DB_USERNAME:-$test_username}"
export CDD_MERCHANT_DB_PASSWORD="${CDD_MERCHANT_DB_PASSWORD:-$test_password}"
export CDD_MERCHANT_DB_DRIVER_CLASS_NAME="${CDD_MERCHANT_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_PRODUCT_DB_URL="${CDD_PRODUCT_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_PRODUCT_DB_USERNAME="${CDD_PRODUCT_DB_USERNAME:-$test_username}"
export CDD_PRODUCT_DB_PASSWORD="${CDD_PRODUCT_DB_PASSWORD:-$test_password}"
export CDD_PRODUCT_DB_DRIVER_CLASS_NAME="${CDD_PRODUCT_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_REPORT_DB_URL="${CDD_REPORT_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_REPORT_DB_USERNAME="${CDD_REPORT_DB_USERNAME:-$test_username}"
export CDD_REPORT_DB_PASSWORD="${CDD_REPORT_DB_PASSWORD:-$test_password}"
export CDD_REPORT_DB_DRIVER_CLASS_NAME="${CDD_REPORT_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_MARKETING_DB_URL="${CDD_MARKETING_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_MARKETING_DB_USERNAME="${CDD_MARKETING_DB_USERNAME:-$test_username}"
export CDD_MARKETING_DB_PASSWORD="${CDD_MARKETING_DB_PASSWORD:-$test_password}"
export CDD_MARKETING_DB_DRIVER_CLASS_NAME="${CDD_MARKETING_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_DECORATION_DB_URL="${CDD_DECORATION_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_DECORATION_DB_USERNAME="${CDD_DECORATION_DB_USERNAME:-$test_username}"
export CDD_DECORATION_DB_PASSWORD="${CDD_DECORATION_DB_PASSWORD:-$test_password}"
export CDD_DECORATION_DB_DRIVER_CLASS_NAME="${CDD_DECORATION_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

export CDD_RELEASE_DB_URL="${CDD_RELEASE_DB_URL:-$CDD_DB_MIGRATION_URL}"
export CDD_RELEASE_DB_USERNAME="${CDD_RELEASE_DB_USERNAME:-$test_username}"
export CDD_RELEASE_DB_PASSWORD="${CDD_RELEASE_DB_PASSWORD:-$test_password}"
export CDD_RELEASE_DB_DRIVER_CLASS_NAME="${CDD_RELEASE_DB_DRIVER_CLASS_NAME:-com.mysql.cj.jdbc.Driver}"

bash "$repo_root/scripts/db/migrate.sh"

echo "MySQL 测试库已重建并完成迁移：${test_database}"
