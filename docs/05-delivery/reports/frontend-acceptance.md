# 前端验收报告

| 项目 | 说明 |
| --- | --- |
| 目标 | 确保 `cdd-frontend` 作为一期后端补充交付，安装/构建/运行/探活可行 |
| 验收时间 | 2026-03-23 |
| 验收人 | 主 agent + 多模型 sub-agent 协作 |
| 相关文档 | [前端交付与验收说明](../前端交付与验收说明.md)、`cdd-frontend/README.md` |

## 执行命令

| 场景 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| 安装依赖 | `corepack pnpm install` | 成功 | 依赖已安装，`pnpm-lock.yaml` 已生成 |
| 生产构建 | `corepack pnpm build` | 成功 | 2026-03-22 复验通过，`vue-tsc -b && vite build` 完成 |
| 开发服务器 | `corepack pnpm dev --host 127.0.0.1 --port 4173` | 成功 | `curl -I -L http://127.0.0.1:4173/` 返回 `HTTP/1.1 200 OK` |
| 本地基础设施 | `bash scripts/local/up_local_infra.sh` | 成功 | Docker 已拉起 `cdd-local-mysql` 与 `cdd-local-nacos` |
| 数据库迁移 | `bash scripts/db/migrate.sh` | 成功 | Liquibase 成功执行到 `V21`，补齐商品、订单、报表与工作台演示数据 |

## 联调状态

- 认证服务健康检查：`GET http://127.0.0.1:8081/actuator/health` 返回 `{"status":"UP"}`。
- 登录接口：`POST http://127.0.0.1:8081/api/auth/merchant/login` 调用成功，返回 `code=0`，成功拿到 `access_token` / `refresh_token`。
- 当前身份接口：`GET http://127.0.0.1:8081/api/auth/me` 调用成功，返回 `merchant_id=merchant_1001`、`store_id=store_1001`、`role_codes=["merchant_owner"]`。
- 前端代理登录链路：`POST http://127.0.0.1:4173/api/auth/merchant/login` 调用成功，说明前端代理到认证服务链路可用。
- 前端代理当前身份链路：`GET http://127.0.0.1:4173/api/auth/me` 调用成功，返回结果与直连认证服务一致。
- 商品列表：`GET http://127.0.0.1:8084/api/product/spu?merchant_id=1001&store_id=1001` 调用成功，返回 3 条商品数据，覆盖 `on_shelf / draft / off_shelf` 三种状态。
- 订单列表：`GET http://127.0.0.1:8085/api/order/orders?merchant_id=1001&store_id=1001&user_id=1001` 调用成功，返回 2 条订单数据，覆盖 `paid / completed` 状态。
- 前端代理商品链路：`GET http://127.0.0.1:4173/api/product/spu?merchant_id=1001&store_id=1001` 调用成功，返回结果与直连商品服务一致。
- 前端代理订单链路：`GET http://127.0.0.1:4173/api/order/orders?merchant_id=1001&store_id=1001&user_id=1001` 调用成功，返回结果与直连订单服务一致。
- 报表服务健康检查：`GET http://127.0.0.1:8088/actuator/health` 返回 `{"status":"UP"}`。
- 商家看板接口：`GET http://127.0.0.1:8088/api/report/merchant-dashboard/latest?merchant_id=1001&store_id=1001` 调用成功，返回最新快照时间 `2026-03-22 12:00:00`。
- 前端代理工作台报表链路：`GET http://127.0.0.1:4173/api/report/merchant-dashboard/latest?merchant_id=1001&store_id=1001` 调用成功，说明前端已可通过 `report-service` 获取真实工作台指标。
- 前端页面探活：`GET http://127.0.0.1:4173/dashboard` 返回 `200 OK`，工作台页面可访问。
- 配置服务存活检查：`GET http://127.0.0.1:8089/actuator/health/liveness` 与 `readiness` 均返回 `{"status":"UP"}`。
- 配置中心真实链路：`GET http://127.0.0.1:8089/api/config/merchant/feature-switches?merchant_id=merchant_1001` 调用成功，返回 4 条商家功能开关数据。
- 前端代理配置链路：`GET http://127.0.0.1:4173/api/config/platform/kv/effective?merchant_id=merchant_1001&config_group=system&config_key=default_time_zone` 调用成功，返回 `Asia/Shanghai`。
- 前端页面探活：`GET http://127.0.0.1:4173/config` 返回 `200 OK`，配置中心页面可访问。
- 售后页已接真实接口：售后列表、详情、日志与审核面板均基于 `order-service` 真数据返回。
- 配置发布记录已接真实接口：发布记录列表、详情、发起发布与回滚不再依赖前端演示数据。
- 本地基础设施已补齐 `Redis`，`order-service` 与 `config-service` 在 `local` 环境下的 `/actuator/health` 可返回 `UP`。

## 2026-03-23 真实烟测补充

- 商品服务随机端口实例：`GET /api/product/spu`、`GET /api/product/spu/{product_id}`、`PUT /api/product/spu/{product_id}` 已完成真实烟测；同时验证了同商品连续编辑两次不再触发 SKU 唯一键冲突。
- 订单服务随机端口实例：`GET /api/order/orders`、`GET /api/order/after-sales`、`GET /api/order/after-sales/{after_sale_no}`、`GET /api/order/after-sales/{after_sale_no}/logs`、`GET /api/order/orders/export` 已完成真实烟测。
- 报表服务随机端口实例：`GET /api/report/data-health`、`GET /api/report/health?merchant_id=1001&store_id=1001` 已完成真实烟测。
- 配置服务随机端口实例：`GET /api/config/publish-records`、`GET /api/config/publish-records/{task_no}`、`POST /api/config/publish-records`、`POST /api/config/publish-records/{task_no}/rollback` 已完成真实烟测。
- 当前本地真实联调依赖 `MySQL + Redis + Nacos` 全部拉起；缺少任一基础设施都可能导致健康检查或部分链路异常。

## 风险与未覆盖

1. 商品、认证、订单链路已统一到本地 MySQL，但商品模板定义仍保留服务内默认模板；若后续要把模板节点也落库，需要追加独立迁移与持久化实现。
2. 当前验收已覆盖登录、商品、订单、售后、工作台报表、配置中心与页面可用性，但仍未覆盖网关聚合、消息链路、异步补偿与多角色权限组合场景。
3. 工作台当前直接对接 `report-service` 的 `/api/report/*` 路径；商家端与平台端文档中的 `/merchant/dashboard/*`、`/platform/dashboard/*` 聚合口径尚未落到网关聚合层。
4. 若更换开发机或重置本地容器环境，需先重新执行 `bash scripts/local/up_local_infra.sh`，确保 `MySQL + Redis + Nacos` 全部就绪。
5. 页面还未接入后端分页、复杂筛选与更细粒度状态变更，后续改动需重新执行本流程并更新报告。
