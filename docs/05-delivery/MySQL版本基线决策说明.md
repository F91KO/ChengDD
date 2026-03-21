# MySQL版本基线决策说明

## 1. 背景

- 当前项目数据库设计文档按 `MySQL 5.7` 口径编写，核心诉求是降低私有化交付门槛。
- 原迁移链路采用 `Spring Boot 3.3.2 + Flyway`，本地迁移通过 `cdd-db-migration` 执行。
- 2026-03-21 本地验证时，`Flyway` 在连接 `MySQL 5.7` 时直接报错：`Unsupported Database: MySQL 5.7`。
- 当前决策不是升级数据库版本，而是保留 `MySQL 5.7` 基线，并将迁移工具切换为 `Liquibase`。

## 2. 决策目标

- 明确当前项目的数据库运行基线。
- 避免继续出现“文档写 5.7、工具链按 8.0、环境验证跑不通”的分裂状态。
- 在研发效率、工具兼容性、私有化交付成本之间选一个当前阶段最稳的方案。

## 3. 决策结果

当前已选方案：

- 数据库运行基线继续维持 `MySQL 5.7`
- 数据库迁移工具从 `Flyway` 切换为 `Liquibase`
- 继续保留版本化 SQL 目录 `db/migration/`
- 由 `Liquibase` 主清单显式编排各版本 SQL 的执行顺序

## 4. 这样选的原因

- 数据库版本不是当前真正的问题，问题是迁移工具与 `MySQL 5.7` 不兼容。
- 如果为了保住 `Flyway` 而强行把运行基线改到 `MySQL 8.0`，会直接提高私有化交付门槛。
- `Liquibase` 可以继续承接版本化 SQL 迁移，不需要推翻现有 `db/migration/` 结构。
- 这样处理后，文档、交付口径、数据库运行环境可以继续围绕 `MySQL 5.7` 保持一致。

## 5. 关键影响

- `local / dev / test / prod` 继续按 `MySQL 5.7` 作为默认数据库口径。
- `cdd-db-migration` 不再依赖 `Flyway`，改为 `Liquibase` 统一执行迁移。
- 现有版本 SQL 文件仍保留在 `db/migration/`，无需整体重写成 XML 或 YAML。
- 后续新增迁移脚本时，需要同时把新版本登记到 `config/db-migration/db.changelog-master.yaml`。

## 7. 建议落地口径

- 当前默认口径：
  - 运行基线：`MySQL 5.7`
  - 字符集：`utf8mb4`
  - 表结构策略：逻辑删除、无物理外键约束
  - 迁移工具：`Liquibase`
  - SQL 设计原则：不依赖 `MySQL 8` 专属特性

## 8. 后续动作

1. 统一文档中的数据库运行基线表述。
2. `cdd-db-migration` 从 `Flyway` 切换为 `Liquibase`。
3. 用 `MySQL 5.7` 重新跑通 `bash scripts/db/migrate.sh`。
4. 在后续新增迁移版本时，保持 SQL 文件与 `Liquibase` 主清单同步更新。
