# ChengDD Backend Skeleton

Java Spring 多模块后端骨架已经按 `cdd-parent` 聚合完成，技术基线固定为 `JDK 17`。

## Layout

仓库当前采用“根目录承载文档与脚本，`cdd-parent/` 承载所有 Java 模块”的物理结构：

```text
.
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

## Build

```bash
mvn -f cdd-parent/pom.xml clean install
```

## Validation

```bash
bash scripts/validation/validate_backend_skeleton.sh
```

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
