# 服务级 Agent 清单

## 1. 说明

- 本文用于单独汇总当前仓库推荐使用的服务级 agent。
- 目标是让你能快速看到“这个 agent 负责什么、可以改哪里、怎么验证”。
- 主 agent 负责任务拆解、分派和最终验收；本文清单主要针对编码 sub-agent 与服务级分析/测试场景。

## 2. 使用方式

- 当任务边界已经明确到某个服务时，优先按服务启动对应 agent。
- 当任务跨多个服务时，由主 agent 先拆成多个服务级子任务，再分别派发。
- 当多个服务级任务写入范围不冲突时，默认并行启动对应 agent。
- 当线上或联调问题集中在某个服务时，优先启动对应服务的分析 agent。

## 3. 命名规则

- 编码 agent：`<service>-coding-agent`
- 测试 agent：`<service>-testing-agent`
- 分析 agent：`<service>-analysis-agent`

例如：

- `order-service-coding-agent`
- `merchant-service-testing-agent`
- `gateway-analysis-agent`

## 4. 服务级清单

| 服务/模块 | 推荐 agent 名称 | 主要职责 | 建议写入范围 | 推荐模型 | 常用验证命令 |
| --- | --- | --- | --- | --- | --- |
| `cdd-auth-service` | `auth-service-coding-agent` | 登录、刷新、登出、认证令牌、会话相关能力 | `cdd-parent/cdd-auth-service/**`、必要时 `cdd-parent/cdd-api-auth/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-auth-service -am test` |
| `cdd-gateway` | `gateway-coding-agent` | 网关鉴权、上下文透传、统一入口能力 | `cdd-parent/cdd-gateway/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-gateway -am test` |
| `cdd-merchant-service` | `merchant-service-coding-agent` | 商家入驻、审核、一键开通、商家基础资料 | `cdd-parent/cdd-merchant-service/**`、必要时 `cdd-parent/cdd-api-merchant/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-merchant-service -am test` |
| `cdd-product-service` | `product-service-coding-agent` | 分类模板、商家分类树、商品目录、库存能力 | `cdd-parent/cdd-product-service/**`、必要时 `cdd-parent/cdd-api-product/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-product-service -am test` |
| `cdd-order-service` | `order-service-coding-agent` | 购物车、结算、下单、支付、退款、补偿、订单流转 | `cdd-parent/cdd-order-service/**`、必要时 `cdd-parent/cdd-api-order/**`、`cdd-parent/cdd-api-pay/**` | `gpt-5.4` 优先 | `mvn -q -f cdd-parent/pom.xml -pl cdd-order-service -am test` |
| `cdd-release-service` | `release-service-coding-agent` | 小程序发布、回滚、版本映射、发布治理 | `cdd-parent/cdd-release-service/**`、必要时 `cdd-parent/cdd-api-release/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-release-service -am test` |
| `cdd-config-service` | `config-service-coding-agent` | 配置项、功能开关、商家级覆盖 | `cdd-parent/cdd-config-service/**`、必要时 `cdd-parent/cdd-api-config/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-config-service -am test` |
| `cdd-common-security` | `common-security-coding-agent` | RBAC、数据权限、登录上下文、安全拦截器 | `cdd-parent/cdd-common-security/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-common-security -am test` |
| `cdd-agent-core` | `agent-core-coding-agent` | 任务定义、执行、补偿、审批、审计底座 | `cdd-parent/cdd-agent-core/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-agent-core -am test` |

## 5. 推荐使用原则

- 一个编码 agent 默认只负责一个服务或一个公共模块。
- 不建议让多个编码 agent 同时修改同一组核心文件。
- 跨服务需求由主 agent 先拆解，再派发到不同服务 agent。
- 提交、PR、合并前，必须有测试 agent 或统一测试流程产出的报告。

## 6. 与主 agent 的协作边界

主 agent 负责：

- 输出任务 list
- 明确验收标准
- 决定启动哪些服务级 agent
- 收口多 agent 结果
- 做最终验收结论

服务级 agent 负责：

- 在既定服务边界内编码、测试或分析
- 回传本次变更范围
- 回传建议验证点或失败根因

## 7. 入口文档

- 详细协作规范见 [测试Sub-Agent执行规范.md](./测试Sub-Agent执行规范.md)
- GitHub 任务和分支执行流程见 [GitHub骨架任务执行说明.md](./GitHub骨架任务执行说明.md)
