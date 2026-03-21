# ChengDaDa Database Migrations

数据库正式迁移脚本按版本化 SQL 组织在 `db/migration/`，由 `Liquibase` 统一编排执行。

当前版本划分：

- `V1__auth_and_config.sql`
- `V2__merchant.sql`
- `V3__decoration.sql`
- `V4__product.sql`
- `V5__order.sql`
- `V6__release.sql`
- `V7__marketing_and_report.sql`
- `V8__seed_data.sql`
- `V9__idempotency_and_compensation.sql`
- `V10__auth_persistence.sql`
- `V11__auth_seed_accounts.sql`

说明：

- 迁移内容基于 [docs/schema-draft.sql](/Volumes/workspace/ChengDD/docs/schema-draft.sql) 拆分，并吸收了数据库细化文档中的约束调整。
- 当前运行基线采用 MySQL 5.7、`utf8mb4`、逻辑删除、无物理外键约束。
- 若后续继续演进字段或索引，不回改已有版本，新增更高版本迁移脚本。
- 迁移执行入口不再依赖根 `pom.xml` 中的数据库连接参数，统一改为独立配置文件：`config/db-migration/application-db-migration.yml`
- `Liquibase` 主清单位于：`config/db-migration/db.changelog-master.yaml`
- 执行方式：

```bash
bash scripts/db/migrate.sh
```
