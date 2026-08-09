#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$repo_root/infrastructure/local/docker-compose.yml"

if [[ ! -f "$compose_file" ]]; then
  echo "Compose file not found: $compose_file" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "未检测到 docker compose 或 docker-compose，请先安装 Docker Desktop。" >&2
  exit 1
fi

"${COMPOSE_CMD[@]}" -f "$compose_file" up -d

echo "Waiting for Nacos to become healthy..."
nacos_health_url="http://127.0.0.1:${CDD_LOCAL_NACOS_CONSOLE_PORT:-8080}/v3/console/health/liveness"
for _ in {1..60}; do
  if curl --silent --fail "$nacos_health_url" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

if ! curl --silent --show-error --fail "$nacos_health_url" >/dev/null; then
  echo "Nacos did not become healthy: $nacos_health_url" >&2
  exit 1
fi

echo
echo "本地基础设施已启动。"
echo "MySQL: 127.0.0.1:${CDD_LOCAL_MYSQL_PORT:-3306}  数据库: ${CDD_LOCAL_MYSQL_DATABASE:-chengdd}"
echo "Nacos Console: http://127.0.0.1:${CDD_LOCAL_NACOS_CONSOLE_PORT:-8080}/index.html"
echo "Nacos Client API: 127.0.0.1:${CDD_LOCAL_NACOS_PORT:-8848}"
echo "Redis: 127.0.0.1:${CDD_LOCAL_REDIS_PORT:-6379}"
echo
echo "下一步建议："
echo "1. 查看状态: bash scripts/local/status_local_infra.sh"
echo "2. 执行迁移: bash scripts/db/migrate.sh"
