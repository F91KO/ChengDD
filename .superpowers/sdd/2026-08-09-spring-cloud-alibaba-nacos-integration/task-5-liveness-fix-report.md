# Task 5 Nacos liveness fix report

## Root cause

Nacos Server 3.0.3 exposes Console liveness on the Console port at
`/v3/console/health/liveness`. The local Compose healthcheck and
`up_local_infra.sh` instead queried the removed v1 Console route through the
client API port (8848), leaving the container unhealthy and making the startup
script time out.

## Change

- The in-container Compose healthcheck now queries
  `http://127.0.0.1:8080/v3/console/health/liveness`.
- The startup script now polls the externally mapped Console port from
  `CDD_LOCAL_NACOS_CONSOLE_PORT` (default `8080`) at the same v3 endpoint.
- Added a focused scripted contract that proves an overridden Console port is
  used, preserves Console/API output, and validates the rendered Compose
  healthcheck.

## Verification

RED (before the change):

```text
Nacos did not become healthy: http://127.0.0.1:8848/nacos/v1/console/health/liveness
```

GREEN:

```text
bash scripts/testing/test_local_infra_nacos_liveness.sh
local infrastructure Nacos liveness checks passed

docker compose -f infrastructure/local/docker-compose.yml config
bash -n scripts/local/up_local_infra.sh
bash -n scripts/testing/test_local_infra_nacos_liveness.sh
```

Real Nacos 3.0.3 verification after recreating only `cdd-local-nacos`:

```text
old endpoint: HTTP 500 (no static resource)
new endpoint: HTTP 200 {"code":0,"message":"success","data":"ok"}
bash scripts/local/up_local_infra.sh: completed successfully
docker inspect cdd-local-nacos health: healthy
```
