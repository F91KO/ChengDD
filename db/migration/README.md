# ChengDaDa Database Migrations

数据库正式迁移脚本按 Flyway 风格组织在 `db/migration/`。

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

说明：

- 迁移内容基于 [docs/schema-draft.sql](/Volumes/workspace/ChengDD/docs/schema-draft.sql) 拆分，并吸收了数据库细化文档中的约束调整。
- 当前仍采用 MySQL 5.7、`utf8mb4`、逻辑删除、无物理外键约束。
- 若后续继续演进字段或索引，不回改已有版本，新增更高版本迁移脚本。
- 迁移执行入口不再依赖根 `pom.xml` 中的数据库连接参数，统一改为独立配置文件：`config/db-migration/application-db-migration.yml`
- 执行方式：

```bash
bash scripts/db/migrate.sh
```
