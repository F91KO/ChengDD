# cdd-frontend

一期商家后台前端工程，技术栈固定为：

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios
- CSS Modules
- ECharts

## Run

安装依赖：

```bash
corepack pnpm install
```

启动开发环境：

```bash
corepack pnpm dev --host 127.0.0.1 --port 4173
```

构建：

```bash
corepack pnpm build
```

## 验收与报告

前端属于一期后端收口的补充验证，必须输出一份测试报告并归档在 `docs/05-delivery/reports/frontend-acceptance.md`。

| 场景 | 命令 | 状态 |
| --- | --- | --- |
| 依赖安装 | `corepack pnpm install` | 已执行 |
| 生产构建 | `corepack pnpm build` | 已执行 |
| 开发服务器 & 探活 | `corepack pnpm dev --host 127.0.0.1 --port 4173` + `curl -I -L http://127.0.0.1:4173/` | 已执行 |

在入口文档和交付清单中说明这一套流程，后续若要替换真实接口或新增页面，应回写此报告并更新 `docs/05-delivery/当前任务收口清单.md`。

## Route Map

- `/login` 登录页
- `/dashboard` 工作台
- `/products` 商品管理
- `/orders` 订单管理
- `/aftersales` 售后处理
- `/config` 配置中心

## Current Scope

- 当前采用“真实接口优先 + mock fallback”模式：
  - 登录、当前身份走 `/api/auth/*`
  - 商品列表走 `/api/product/spu`
  - 订单列表走 `/api/order/orders`
  - 工作台报表走 `/api/report/*`
  - 配置中心走 `/api/config/*`
- 本地开发默认通过 Vite proxy 转发：
  - `/api/auth` -> `127.0.0.1:8081`
  - `/api/product` -> `127.0.0.1:8084`
  - `/api/order` -> `127.0.0.1:8085`
  - `/api/report` -> `127.0.0.1:8088`
  - `/api/config` -> `127.0.0.1:8089`
- 已接入路由守卫、登录态持久化、Axios 请求拦截、401 刷新与登录跳转
- 当 `merchant_id/store_id/user_id` 无法从认证上下文解析为数值时，会回退到 `.env` 默认值或演示数据
- 工作台已接入 `report-service` 真实接口，配置中心已接入 `config-service` 真实接口，售后页当前仍以页面演示为主，后续继续补真实接口
