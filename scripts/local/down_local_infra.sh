#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$repo_root/infrastructure/local/docker-compose.yml"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "未检测到 docker compose 或 docker-compose，请先安装 Docker Desktop。" >&2
  exit 1
fi

"${COMPOSE_CMD[@]}" -f "$compose_file" down
