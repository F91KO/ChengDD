# ChengDD Project Skeleton

Java Spring 多模块后端骨架已经按 `cdd-parent` 聚合完成，技术基线固定为 `JDK 17`；前端一期后台控制台已落地到 `cdd-frontend/`。

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

```bash
mvn -f cdd-parent/pom.xml clean install
```

## Backend Validation

```bash
bash scripts/validation/validate_backend_skeleton.sh
```

## Backend Test

统一测试基线使用本地 MySQL 测试库 `chengdd_test`。执行前先确保本地 MySQL 基础设施可用，再执行：

```bash
bash scripts/testing/run_phase1_acceptance.sh
```

如果只想单独准备测试库：

```bash
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
- Runtime property keys: `cdd.runtime.env` / `cdd.runtime.config-mode`
- Environment variable overrides: `CDD_ENV` / `CDD_CONFIG_MODE`
- Each executable module keeps:
  - `application.yaml`
  - `application-local.yaml`
  - `application-dev.yaml`
  - `application-test.yaml`
  - `application-prod.yaml`

当前默认仍走服务内文件配置；后续接 Nacos 时，沿用相同的 `cdd.runtime.*` 键和环境命名规则继续演进，服务 `dataId` 采用 `{service-name}-{env}.yaml`，共享配置采用 `cdd-common-{env}.yaml`。

## Flyway Migration

迁移脚本统一使用仓库根目录的 `db/migration`，数据库连接配置已外置到独立 `yml`：

```bash
bash scripts/db/migrate.sh
```

默认配置文件：

```bash
config/db-migration/application-db-migration.yml
```

后续如果接入 Nacos，可以继续沿用这套 Spring Boot 配置键，将本地 `yml` 迁移为远端配置中心；`cdd-db-migration` 仍保持独立文件配置，不并入 Nacos。

## Modules

- `cdd-common-*`：公共基础能力
- `cdd-db-migration`：数据库迁移执行模块
- `cdd-api-*`：服务间协议对象
- `cdd-pay-core`：支付抽象能力
- `cdd-gateway`、`cdd-*-service`：网关与服务骨架
