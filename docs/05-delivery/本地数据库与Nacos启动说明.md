# 本地数据库、Redis 与 Nacos 启动说明

## 1. 目标

- 在本地一键拉起 `MySQL + Redis + Nacos` 基础设施。
- 为数据库迁移、后续服务切换到本地 MySQL、以及未来 Nacos 接入做好准备。
- 保持当前项目默认验收路径不强依赖 Nacos 可用。

## 2. 当前约定

- 本地数据库：`MySQL 5.7`
- 本地缓存：`Redis 7.2`
- 本地配置中心：`Nacos 2.3.2`
- 容器编排文件：`infrastructure/local/docker-compose.yml`
- 本地环境仍遵循《[Nacos配置命名与加载约定.md](../02-architecture/Nacos配置命名与加载约定.md)》：
  - `local` 默认允许只走文件配置
  - `dev / test / prod` 才是后续 Nacos 主要接入路径

## 3. 启动方式

启动本地基础设施：

```bash
bash scripts/local/up_local_infra.sh
```

如果本机 `3306` 已被占用，先改一个本地映射端口再启动：

```bash
export CDD_LOCAL_MYSQL_PORT=3307
bash scripts/local/up_local_infra.sh
```

查看状态：

```bash
bash scripts/local/status_local_infra.sh
```

停止基础设施：

```bash
bash scripts/local/down_local_infra.sh
```

## 4. 默认端口与账号

### 4.1 MySQL

- 地址：`127.0.0.1:${CDD_LOCAL_MYSQL_PORT:-3306}`
- 数据库：`chengdd`
- 用户名：`root`
- 密码：`change_me`

当前与迁移配置文件保持一致：

- [application-db-migration.yml](/Volumes/workspace/ChengDD/config/db-migration/application-db-migration.yml)

补充说明：

- 当前运行时基线为 `MySQL 5.7`
- 数据库迁移由 `Liquibase` 执行，主清单位于 `config/db-migration/db.changelog-master.yaml`
- `local` 环境下的 `auth-service`、`decoration-service`、`marketing-service`、`order-service`、`release-service`、`report-service`、`product-service`、`merchant-service`、`config-service` 默认也统一连接本地 MySQL，不再走 H2

### 4.2 Nacos

- 控制台地址：`http://127.0.0.1:8848/nacos`
- 当前本地 compose 采用 `standalone` 模式
- 当前本地 compose 关闭鉴权，便于先完成本地联调

### 4.3 Redis

- 地址：`127.0.0.1:${CDD_LOCAL_REDIS_PORT:-6379}`
- 当前本地 compose 默认开启 AOF 持久化
- `order-service`、`config-service` 等依赖 `cdd-common-redis` 的服务，本地 actuator 健康检查依赖 Redis 可达

## 5. 初始化数据库

基础设施启动后，执行：

```bash
bash scripts/db/migrate.sh
```

该脚本会读取：

- [application-db-migration.yml](/Volumes/workspace/ChengDD/config/db-migration/application-db-migration.yml)

并通过 `cdd-db-migration` 执行 `db/migration/` 下的 Liquibase 迁移编排。

`db-migration` 会默认跟随以下环境变量：

- `CDD_LOCAL_MYSQL_PORT`
- `CDD_LOCAL_MYSQL_DATABASE`
- `CDD_LOCAL_MYSQL_ROOT_PASSWORD`

如需单独覆盖迁移连接，也可以直接指定：

```bash
export CDD_DB_MIGRATION_URL='jdbc:mysql://127.0.0.1:3307/chengdd?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'
export CDD_DB_MIGRATION_USERNAME='root'
export CDD_DB_MIGRATION_PASSWORD='change_me'
```

## 6. 本地服务数据库约定

当前 `local` 环境下，以下服务默认直接连接本地 MySQL：

- `auth-service`
- `decoration-service`
- `marketing-service`
- `order-service`
- `release-service`
- `report-service`
- `product-service`
- `merchant-service`
- `config-service`

默认连接参数：

- 地址：`127.0.0.1:${CDD_LOCAL_MYSQL_PORT:-3306}`
- 数据库：`${CDD_LOCAL_MYSQL_DATABASE:-chengdd}`
- 用户名：`${CDD_LOCAL_MYSQL_USERNAME:-root}`
- 密码：`${CDD_LOCAL_MYSQL_ROOT_PASSWORD:-change_me}`

如果你需要单独覆盖某个服务，也可以在启动前覆盖：

```bash
export CDD_AUTH_DB_URL='jdbc:mysql://127.0.0.1:3306/chengdd?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'
export CDD_AUTH_DB_USERNAME='root'
export CDD_AUTH_DB_PASSWORD='change_me'
export CDD_AUTH_DB_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
export CDD_AUTH_SQL_INIT_MODE='never'
```

说明：

- 本地表结构与演示数据应先通过 `bash scripts/db/migrate.sh` 完成迁移
- `V18__local_demo_product_order_seed.sql` 会补齐本地联调用的商品与订单演示数据
- `V19__local_demo_product_seed_patch.sql` 会兼容已执行过旧版 `V18` 的数据库，并补齐商品演示数据
- `V20__local_demo_report_seed.sql` 与 `V21__local_demo_dashboard_refresh.sql` 会补齐并刷新工作台报表演示数据，避免旧的手工验收数据影响本地工作台展示
- `auth-service` 登录账号由正式迁移脚本种子提供，不再依赖本地 H2 初始化

也可以直接使用启动脚本：

```bash
bash scripts/local/run_auth_service_mysql.sh
```

默认行为：

- 连接 `127.0.0.1:${CDD_LOCAL_MYSQL_PORT:-3306}`
- 数据库名使用 `${CDD_LOCAL_MYSQL_DATABASE:-chengdd}`
- 用户名默认 `${CDD_LOCAL_MYSQL_USERNAME:-root}`
- 关闭 SQL 初始化脚本，直接连 MySQL 启动 `auth-service`

## 7. 可覆盖环境变量

如需改端口或数据库名，可在执行脚本前覆盖：

```bash
export CDD_LOCAL_MYSQL_PORT=3307
export CDD_LOCAL_MYSQL_DATABASE=chengdd_local
export CDD_LOCAL_MYSQL_ROOT_PASSWORD=change_me
export CDD_LOCAL_REDIS_PORT=6379
export CDD_LOCAL_NACOS_PORT=8858
export CDD_LOCAL_NACOS_GRPC_PORT=9858
export CDD_LOCAL_NACOS_RAFT_PORT=9859
```

## 8. 启动本地服务

启动 `auth-service`：

```bash
bash scripts/local/run_auth_service_mysql.sh
```

启动 `order-service`：

```bash
bash scripts/local/run_order_service_mysql.sh
```

启动 `decoration-service`：

```bash
bash scripts/local/run_decoration_service_mysql.sh
```

启动 `marketing-service`：

```bash
bash scripts/local/run_marketing_service_mysql.sh
```

启动 `release-service`：

```bash
bash scripts/local/run_release_service_mysql.sh
```

启动 `report-service`：

```bash
bash scripts/local/run_report_service_mysql.sh
```

启动 `config-service`：

```bash
bash scripts/local/run_config_service_mysql.sh
```

启动 `product-service`：

```bash
bash scripts/local/run_product_service_mysql.sh
```

启动 `gateway`：

```bash
bash scripts/local/run_gateway.sh
```

默认端口：

- `gateway`: `127.0.0.1:8080`
- `auth-service`: `127.0.0.1:8081`
- `decoration-service`: `127.0.0.1:8083`
- `product-service`: `127.0.0.1:8084`
- `order-service`: `127.0.0.1:8085`
- `marketing-service`: `127.0.0.1:8086`
- `release-service`: `127.0.0.1:8087`
- `report-service`: `127.0.0.1:8088`
- `config-service`: `127.0.0.1:8089`

已验证：

- `decoration-service` 可在 `local + MySQL` 下启动，并通过装修草稿保存、查询、发布接口完成本地联调
- `marketing-service` 可在 `local + MySQL` 下启动，并通过优惠券、活动、推荐规则、专题会场接口完成本地联调
- `report-service` 可在 `local + MySQL` 下启动，并通过首页事件日报、订单日报、商品日报、商家看板、平台看板接口完成本地联调
- `config-service` 可在 `local + MySQL` 下启动，并通过商家功能开关列表、生效配置查询接口完成本地联调
- `config-service` 与 `order-service` 的 actuator 健康检查依赖本地 Redis；若 Redis 未启动，业务接口仍可访问，但 `/actuator/health` 会显示 `DOWN`
- `product-service` 可在 `local + MySQL` 下启动，并通过 `GET /api/product/spu?merchant_id=1001&store_id=1001` 返回 3 条演示商品，状态覆盖 `on_shelf / draft / off_shelf`

当前本地联调测试账号：

- 平台账号：`platform_admin` / `admin123456`
- 商家账号：`merchant_admin` / `merchant123456`

常用检查：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

## 9. 后续接入建议

- 当前阶段本地数据库已作为主要联调基线，优先保证 `local` 与 `test` 的 MySQL 口径一致。
- Nacos 优先用于：
  - 验证 namespace / group / dataId 规则
  - 准备后续 `dev / test / prod` 配置中心接入
- 等 Nacos 真正接入代码时，再单独补：
  - namespace 初始化
  - group 固化
  - dataId 导入脚本
  - 配置变更回滚与审计
