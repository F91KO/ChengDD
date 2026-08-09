# 本地数据库、Redis 与 Nacos 启动说明

## 1. 基线与用途

本地 Compose 提供：

| Component | Image | Default host port |
| --- | --- | ---: |
| MySQL | `mysql:5.7.44` | 3306 |
| Redis | `redis:7.2.5-alpine` | 6379 |
| Nacos | `nacos/nacos-server:v3.0.3` | Console 8080、Client 8848、gRPC 9848 |

Nacos 使用 standalone、关闭鉴权，只用于本地开发和自动化验收。它不是生产模板；生产集群、高可用、持久化设计、鉴权、权限、TLS 和密钥轮换均需另行建设。

## 2. 启停基础设施

单独启动 Compose：

```bash
bash scripts/local/up_local_infra.sh
bash scripts/local/status_local_infra.sh
```

此时 Nacos Console 为 `http://127.0.0.1:8080/index.html`，Client API 为 `127.0.0.1:8848`，gRPC 为 `9848`；健康检查使用 Nacos 3 的 `/v3/console/health/liveness`。

全后端同机运行时，Gateway 也占用 `8080`，因此 `run_all_services_mysql.sh` 默认把 Nacos Console 的宿主机端口覆盖为 `18080`：

```bash
CDD_ENV=local CDD_CONFIG_MODE=nacos bash scripts/local/run_all_services_mysql.sh
```

已用默认 8080 启动过 Nacos 时，先统一按同一端口设置重建，避免 Compose 端口配置与当前容器不一致：

```bash
export CDD_LOCAL_NACOS_CONSOLE_PORT=18080
bash scripts/local/up_local_infra.sh
```

停止全部基础设施：

```bash
bash scripts/local/down_local_infra.sh
```

## 3. 可覆盖端口

```bash
export CDD_LOCAL_MYSQL_PORT=3307
export CDD_LOCAL_REDIS_PORT=6380
export CDD_LOCAL_NACOS_CONSOLE_PORT=18080
export CDD_LOCAL_NACOS_PORT=8858
export CDD_LOCAL_NACOS_GRPC_PORT=9858
```

没有额外的 Nacos Raft 宿主机端口映射。应用侧 `CDD_NACOS_SERVER_ADDR` 必须与 Client API 映射一致。

## 4. 数据库迁移

默认数据库为 `chengdd`，本地开发默认用户为 `root`、默认密码为 `change_me`。先启动 MySQL，再执行：

```bash
bash scripts/db/migrate.sh
```

迁移模块始终使用文件配置，不依赖 Nacos。Liquibase `4.31.1` 的兼容实现会在构建产物中保留 `config/db-migration/db.changelog-master.yaml` 和 `db/migration/V*.sql` 的 classpath 目录结构；对已经迁移的本地库会正确识别已执行 changeSet。

可按需覆盖：

```bash
export CDD_LOCAL_MYSQL_DATABASE=chengdd_local
export CDD_LOCAL_MYSQL_ROOT_PASSWORD=change_me
export CDD_DB_MIGRATION_URL='jdbc:mysql://127.0.0.1:3307/chengdd_local?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'
export CDD_DB_MIGRATION_USERNAME=root
export CDD_DB_MIGRATION_PASSWORD=change_me
```

## 5. 配置发布与状态检查

本地 group 固定为 `CHENGDD`，namespace 可为空。发布 11 个 DataId：

```bash
bash scripts/nacos/publish_nacos_configs.sh local
```

检查共享配置和十个应用的注册状态：

```bash
bash scripts/nacos/check_nacos_state.sh local
```

非 local 环境必须显式提供 `CDD_NACOS_NAMESPACE`，并让 Config/Discovery 使用同一 namespace ID。

## 6. 服务生命周期

```bash
export CDD_ENV=local
export CDD_CONFIG_MODE=nacos
bash scripts/local/run_all_services_mysql.sh
bash scripts/local/status_all_services.sh
bash scripts/local/stop_all_services.sh
```

服务端口为 Gateway `8080`，auth `8081`，merchant `8082`，decoration `8083`，product `8084`，order `8085`，marketing `8086`，release `8087`，report `8088`，config `8089`。

`status_all_services.sh` 在 nacos 模式同时检查 HTTP 健康和注册实例；`stop_all_services.sh` 只处理运行时状态文件证明归属的进程，并等待注销。2026-08-09 验收确认 10/10 服务可启动、经 Gateway 完成登录及 auth/me、商品、订单、报表、配置请求，停止后 8080–8089 均无监听且 10 个实例全部注销。

已知问题：Nacos 模式服务在成功注销后会记录 `NacosGracefulShutdownDelegate` ERROR/NPE。当前证据表明它未阻止退出、端口清理或注销，但日志缺陷仍待兼容性修复。
