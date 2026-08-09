# ChengDD Project Skeleton

Java Spring 多模块后端骨架已经按 `cdd-parent` 聚合完成，技术基线固定为 `JDK 21`；前端一期后台控制台已落地到 `cdd-frontend/`。

## Layout

仓库当前采用“根目录承载文档与脚本，`cdd-parent/` 承载所有 Java 模块”的物理结构：

```text
.
├─ cdd-frontend/
├─ cdd-parent/
│  ├─ pom.xml
│  ├─ cdd-common-*
│  ├─ cdd-api-*
│  ├─ cdd-db-migration
│  ├─ cdd-pay-core
│  ├─ cdd-gateway
│  └─ cdd-*-service
├─ config/
├─ db/
├─ docs/
└─ scripts/
```

## Backend Build

后端版本基线由 `cdd-parent/pom.xml` 统一管理：

| Component | Version |
| --- | --- |
| JDK | 21 |
| Spring Boot | 3.5.14 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Nacos Client | 3.0.3 |

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -f cdd-parent/pom.xml clean install
```

项目默认使用用户本地 Maven 仓库 `~/.m2/repository`。如需切换仓库路径，可通过 `CDD_MAVEN_REPO` 或 `-Dmaven.repo.local=...` 覆盖。

## Backend Validation

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash scripts/validation/validate_backend_skeleton.sh
```

本地启动后端服务后，可执行下面的脚本检查“当前运行中的服务是否仍与工作区代码一致”。如果后端代码、配置或迁移脚本已变更但服务未重启，脚本会直接提示需要重启的服务：

```bash
bash scripts/local/check_backend_runtime_sync.sh
```

本地 `scripts/local/run_*` 启动脚本已改为直接基于 Maven Reactor 启动目标服务，不再先执行一次 `install` 再启动，避免重复编译和工作区代码与本地仓库产物不一致。

## Backend Test

统一测试基线使用本地 MySQL 测试库 `chengdd_test`。执行前先确保本地 MySQL 基础设施可用，再执行：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash scripts/testing/run_phase1_acceptance.sh
```

如果只想单独准备测试库：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash scripts/testing/prepare_mysql_test_db.sh
```

## Frontend

前端采用 `Vue 3 + Vite + TypeScript + Pinia + Vue Router + Axios + CSS Modules`，当前已完成一期后台最小可运行骨架，并切到“真实接口优先 + mock fallback”模式。

安装依赖：

```bash
cd cdd-frontend
corepack pnpm install
```

本地开发：

```bash
corepack pnpm dev --host 127.0.0.1 --port 4173
```

生产构建：

```bash
corepack pnpm build
```

当前已提供的页面：

- 登录页
- 工作台
- 商品管理
- 订单管理
- 售后处理
- 配置中心

前端作为一期后端交付的补充收口，自动化验收需要列出在 `docs/05-delivery/reports/frontend-acceptance.md` 中。完成 `pnpm install`、`pnpm build` 和一次 `pnpm dev` 探活后，报告会记录是否达标、失败项与风险，并同步到文档索引。当前工作台页面已接入 `report-service` 真实接口，配置中心页面已接入 `config-service` 真实接口，本地联调需同时确保 `8088` 与 `8089` 可用。

## Environment Convention

- Maven profiles: `local` / `dev` / `test` / `prod`
- Spring runtime profiles: `{env},file` or `{env},nacos`
- Runtime property keys: `cdd.runtime.env` / `cdd.runtime.config-mode`
- Environment variable overrides: `CDD_ENV` / `CDD_CONFIG_MODE`
- Each executable module keeps:
  - `application.yaml`
  - `application-local.yaml`
  - `application-dev.yaml`
  - `application-test.yaml`
  - `application-prod.yaml`

本地脚本默认使用 `CDD_ENV=local`、`CDD_CONFIG_MODE=file`。切换为 `nacos` 后，十个可执行模块通过 `cdd-common-nacos` 统一接入 Spring Cloud Alibaba Config/Discovery；先导入共享配置 `cdd-common-{env}.yaml`，再导入服务配置 `{service-name}-{env}.yaml`，两项均固定 `refreshEnabled=false`。Gateway 在 `nacos` profile 下通过 Spring Cloud LoadBalancer 选择服务实例；没有可用实例或处于 `file` profile 时使用 `application.yaml` 中的静态地址。

本地 Nacos 快速验证：

```bash
export CDD_LOCAL_NACOS_CONSOLE_PORT=18080
bash scripts/local/up_local_infra.sh
export CDD_ENV=local
export CDD_CONFIG_MODE=nacos
bash scripts/local/run_all_services_mysql.sh
bash scripts/local/status_all_services.sh
bash scripts/nacos/check_nacos_state.sh local
bash scripts/local/stop_all_services.sh
```

单独执行 Compose 时，Nacos Console 默认使用 `http://127.0.0.1:8080/index.html`；全服务启动脚本默认将宿主机 Console 端口改为 `18080`，避免与 Gateway 的 `8080` 冲突。Nacos Client API 为 `127.0.0.1:8848`，gRPC 为 `9848`。

## Liquibase Migration

迁移脚本统一使用仓库根目录的 `db/migration`，数据库连接配置已外置到独立 `yml`：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash scripts/db/migrate.sh
```

默认配置文件：

```bash
config/db-migration/application-db-migration.yml
```

`cdd-db-migration` 是明确的例外：始终使用文件配置，不依赖 Nacos。为兼容 Liquibase `4.31.1` 的 classpath 解析，构建时会把主清单打包到 `config/db-migration/`、把 SQL 打包到 `db/migration/`；这不改变仓库中的迁移文件来源。

本地 Compose 使用 `nacos/nacos-server:v3.0.3` standalone 且关闭鉴权，只用于开发与验收，不可作为生产部署模板。生产 Nacos 集群、高可用、鉴权、TLS、Nginx/HTTPS 和 CI/CD 仍待实施。

## Modules

- `cdd-common-*`：公共基础能力
- `cdd-db-migration`：数据库迁移执行模块
- `cdd-api-*`：服务间协议对象
- `cdd-pay-core`：支付抽象能力
- `cdd-gateway`、`cdd-*-service`：网关与服务骨架
