# 程哒哒文档导航

## 1. 导航说明

- 当前文档按“设计类”和“落地类”两大方向整理。
- 当前已完成物理迁移，文档已按分类目录落盘。
- 根目录保留 `README.md` 作为总入口。

## 2. 推荐阅读顺序

1. 先看产品与业务范围：`01-product/ecommerce-marketplace-prd.md`
2. 再看系统边界与服务拆分：`02-architecture/技术架构图.md`、`02-architecture/服务与表关系矩阵.md`
3. 再看配置与命名约束：`02-architecture/命名规范.md`、`02-architecture/Nacos配置命名与加载约定.md`
4. 再看数据库主线：`04-database-design/数据库设计草案.md` -> `04-database-design/数据库设计实施细化.md` -> `04-database-design/一期核心表字段级约束清单.md`
5. 再看状态与幂等：`04-database-design/状态流转矩阵与SQL落库说明.md`、`04-database-design/幂等与补偿表设计.md`、`04-database-design/补偿任务执行器与MQ协同设计.md`
6. 最后看正式落地产物：`../db/migration/`

## 3. 设计类文档

### 3.1 产品与业务设计

| 文档 | 阶段 | 说明 |
| --- | --- | --- |
| [ecommerce-marketplace-prd.md](./01-product/ecommerce-marketplace-prd.md) | 设计基线 | 产品范围、里程碑、验收标准 |
| [业务流程图.md](./01-product/业务流程图.md) | 设计基线 | 商家开通、交易、发布等主流程 |
| [角色泳道图.md](./01-product/角色泳道图.md) | 设计基线 | 平台、商家、消费者职责分工 |
| [状态机图.md](./01-product/状态机图.md) | 设计基线 | 业务对象状态机语义图 |

### 3.2 页面与交互设计

| 文档 | 阶段 | 说明 |
| --- | --- | --- |
| [页面地图.md](./01-product/页面地图.md) | 设计基线 | 平台端、商家端、消费者端页面结构 |
| [商家端功能细化.md](./01-product/商家端功能细化.md) | 设计基线 | 商家后台功能拆分 |
| [平台端功能细化.md](./01-product/平台端功能细化.md) | 设计基线 | 平台后台功能拆分 |

### 3.3 架构与技术设计

| 文档 | 阶段 | 说明 |
| --- | --- | --- |
| [技术架构图.md](./02-architecture/技术架构图.md) | 设计基线 | 系统分层、服务拆分、调用关系 |
| [服务与表关系矩阵.md](./02-architecture/服务与表关系矩阵.md) | 设计基线 | 服务边界和主责表划分 |
| [命名规范.md](./02-architecture/命名规范.md) | 设计基线 | 服务、模块、表、字段命名统一规范 |
| [基础能力模块设计.md](./02-architecture/基础能力模块设计.md) | 设计基线 | `cdd-common-*` 的职责边界、首批能力与演进顺序 |
| [Nacos配置命名与加载约定.md](./02-architecture/Nacos配置命名与加载约定.md) | 设计基线 | Nacos 的 namespace、group、dataId 与本地回退规则 |

### 3.4 接口与协议设计

| 文档 | 阶段 | 说明 |
| --- | --- | --- |
| [商家端字段与接口设计.md](./03-interface/商家端字段与接口设计.md) | 设计基线 | 商家端字段与接口视图 |
| [平台端字段与接口设计.md](./03-interface/平台端字段与接口设计.md) | 设计基线 | 平台端字段与接口视图 |
| [chengdada-merchant-openapi.yaml](./03-interface/chengdada-merchant-openapi.yaml) | 协议草案 | 商家端 OpenAPI 草案 |
| [chengdada-platform-openapi.yaml](./03-interface/chengdada-platform-openapi.yaml) | 协议草案 | 平台端 OpenAPI 草案 |
| [chengdada-openapi-core.yaml](./03-interface/chengdada-openapi-core.yaml) | 协议汇总 | 核心 OpenAPI 汇总稿 |

## 4. 落地类文档

### 4.1 数据库落地设计

| 文档 | 阶段 | 说明 |
| --- | --- | --- |
| [数据库设计草案.md](./04-database-design/数据库设计草案.md) | 草案入口 | 数据库第一版逻辑草案 |
| [数据库设计实施细化.md](./04-database-design/数据库设计实施细化.md) | 实施基线 | 一期数据库目标、状态字典、事务与收口顺序 |
| [一期核心表字段级约束清单.md](./04-database-design/一期核心表字段级约束清单.md) | 落地细化 | 字段必填、默认值、唯一性、更新规则 |
| [核心表索引与唯一约束设计.md](./04-database-design/核心表索引与唯一约束设计.md) | 落地细化 | 索引与唯一约束设计 |
| [状态流转矩阵与SQL落库说明.md](./04-database-design/状态流转矩阵与SQL落库说明.md) | 落地细化 | 状态流转到表更新、事务、日志的映射 |
| [幂等与补偿表设计.md](./04-database-design/幂等与补偿表设计.md) | 落地细化 | 回调幂等与补偿任务表设计 |
| [补偿任务执行器与MQ协同设计.md](./04-database-design/补偿任务执行器与MQ协同设计.md) | 落地细化 | DB 任务表、MQ、扫描器、执行器协同方式 |
| [schema-draft.sql](./04-database-design/schema-draft.sql) | 原始草稿 | 初始整库 SQL 草案 |

### 4.2 正式落地产物

| 文档/目录 | 阶段 | 说明 |
| --- | --- | --- |
| [../db/migration/README.md](../db/migration/README.md) | 正式落地 | 迁移脚本说明 |
| [../db/migration/V1__auth_and_config.sql](../db/migration/V1__auth_and_config.sql) | 正式落地 | 认证与配置表 |
| [../db/migration/V2__merchant.sql](../db/migration/V2__merchant.sql) | 正式落地 | 商家域表 |
| [../db/migration/V3__decoration.sql](../db/migration/V3__decoration.sql) | 正式落地 | 装修域表 |
| [../db/migration/V4__product.sql](../db/migration/V4__product.sql) | 正式落地 | 商品域表 |
| [../db/migration/V5__order.sql](../db/migration/V5__order.sql) | 正式落地 | 订单域表 |
| [../db/migration/V6__release.sql](../db/migration/V6__release.sql) | 正式落地 | 发布域表 |
| [../db/migration/V7__marketing_and_report.sql](../db/migration/V7__marketing_and_report.sql) | 正式落地 | 营销与报表表 |
| [../db/migration/V8__seed_data.sql](../db/migration/V8__seed_data.sql) | 正式落地 | 种子数据 |
| [../db/migration/V9__idempotency_and_compensation.sql](../db/migration/V9__idempotency_and_compensation.sql) | 正式落地 | 幂等与补偿表 |
| [../config/db-migration/application-db-migration.yml](../config/db-migration/application-db-migration.yml) | 正式落地 | 数据库迁移独立配置 |

## 5. 交付与商务类文档

| 文档 | 阶段 | 说明 |
| --- | --- | --- |
| [交付套餐报价简版.md](./05-delivery/交付套餐报价简版.md) | 商务交付 | 交付方案与报价简版 |
| [GitHub骨架任务执行说明.md](./05-delivery/GitHub骨架任务执行说明.md) | 执行说明 | 规划分支和 GitHub issue 的落地执行方式 |
| [骨架验证与验收说明.md](./05-delivery/骨架验证与验收说明.md) | 执行说明 | 骨架边界、构建、启动与 CI 验收方式 |

## 6. 当前建议的使用方式

- 讨论需求、范围、页面时，优先看“设计类文档”。
- 讨论数据库、状态流转、幂等补偿、实现顺序时，优先看“落地类文档”。
- 真正建库和初始化环境时，直接以 `db/migration/` 为准。

## 7. 当前目录结构

当前目录结构：

```text
repo/
├─ cdd-parent/
│  ├─ pom.xml
│  ├─ cdd-common-*
│  ├─ cdd-api-*
│  ├─ cdd-db-migration
│  ├─ cdd-pay-core
│  ├─ cdd-gateway
│  └─ cdd-*-service
├─ config/
│  └─ db-migration/
├─ db/
│  └─ migration/
├─ docs/
│  ├─ 01-product/
│  ├─ 02-architecture/
│  ├─ 03-interface/
│  ├─ 04-database-design/
│  ├─ 05-delivery/
│  └─ README.md
└─ scripts/
```
