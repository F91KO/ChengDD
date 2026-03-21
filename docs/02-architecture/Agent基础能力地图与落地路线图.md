# 程哒哒 Agent 基础能力地图与落地路线图

## 1. 文档说明

- 文档用途：定义当前项目如何从“业务系统骨架”演进出可复用的 Agent 基础能力。
- 当前阶段：能力地图、分阶段落地路线、首批任务拆解。
- 适用对象：后端研发、架构设计、测试、实施。

## 2. 当前判断

- 当前项目本质上仍是电商业务系统骨架，不是完整的 Agent 平台。
- 当前项目已经具备一部分 Agent 底座雏形：
  - 幂等控制
  - 补偿任务
  - MQ 协同
  - 状态流转
  - 配置中心
- 当前更合理的目标不是“直接做一个大而全的智能体平台”，而是先落一层通用任务执行底座，再逐步接入 AI 决策能力。

## 3. 总体设计原则

- 先做可复用的任务执行底座，再接入 LLM 或外部 AI 服务。
- Agent 基础能力独立成模块，不直接塞进 `cdd-common-*`。
- 高风险动作必须支持人工审批、人工接管、人工重放。
- 所有任务执行必须有状态、审计、重试、补偿，不允许“黑盒自动执行”。
- 对外提示信息、审计说明、错误原因统一使用中文。
- 第一阶段优先规则明确、可验证、可回放的任务，不做开放式自主规划。

## 4. 模块边界建议

### 4.1 新增模块建议

建议新增独立模块：

- `cdd-agent-core`

职责：

- 任务定义模型
- 任务实例模型
- 步骤实例模型
- 执行器框架
- 工具调用抽象
- 上下文模型
- 审计记录接口
- 审批网关接口
- 补偿处理接口

不放内容：

- 商家、商品、订单等业务规则
- 具体 MQ Topic 的业务命名
- 具体 LLM 厂商 SDK
- 某个服务私有的任务处理逻辑

### 4.2 与现有模块的关系

`cdd-common-core`：

- 继续承载通用错误模型、基础上下文常量、通用枚举
- 不承载 Agent 任务执行框架

`cdd-common-db`：

- 承载任务表、审计表通用数据库基础能力
- 不承载具体任务执行逻辑

`cdd-common-security`：

- 提供操作人、租户、请求头透传能力
- 供 Agent 任务上下文复用

业务服务：

- 只实现各自领域的任务处理器、工具适配器、审批规则
- 不重复造执行器骨架

## 5. 能力地图

### 5.1 第一层：执行底座

核心能力：

- 任务定义 `TaskDefinition`
- 任务实例 `TaskInstance`
- 步骤实例 `TaskStepInstance`
- 执行入口 `TaskExecutor`
- 状态机 `TaskStatus`
- 上下文 `TaskContext`
- 执行日志 `TaskExecutionLog`
- 审计记录 `AuditRecorder`

目标：

- 让任何需要“多步骤、可重试、可审计”的业务流程都有统一承载点

### 5.2 第二层：控制能力

核心能力：

- 重试策略
- 超时控制
- 幂等控制
- 补偿处理
- 抢占执行
- 并发限制
- 人工审批
- 人工取消

目标：

- 让任务不仅能执行，还能在失败、超时、重复触发、人工干预下保持可控

### 5.3 第三层：工具能力

核心能力：

- `ToolInvoker` 抽象
- HTTP 调用工具
- MQ 发送工具
- 内部服务调用工具
- 配置读取工具
- 数据查询工具

目标：

- 将“任务逻辑”和“外部调用方式”解耦，为后续 AI 决策预留统一工具面

### 5.4 第四层：AI 扩展能力

核心能力：

- 任务规划器 `TaskPlanner`
- 参数建议器
- 异常分析器
- 执行建议生成器
- 审批前摘要生成器

目标：

- 让 AI 先做“建议”和“辅助决策”，而不是直接不受控地执行动作

## 6. 最小核心模型

### 6.1 任务定义

建议字段：

- `task_type`
- `task_name`
- `biz_type`
- `max_retry_count`
- `timeout_seconds`
- `requires_approval`
- `compensation_enabled`

### 6.2 任务实例

建议字段：

- `task_no`
- `task_type`
- `task_status`
- `merchant_id`
- `store_id`
- `operator_id`
- `request_id`
- `biz_no`
- `input_json`
- `context_json`
- `result_json`
- `error_code`
- `error_message`
- `retry_count`
- `next_retry_at`
- `started_at`
- `finished_at`

### 6.3 步骤实例

建议字段：

- `task_no`
- `step_code`
- `step_name`
- `step_order`
- `step_status`
- `tool_code`
- `input_json`
- `output_json`
- `error_message`
- `started_at`
- `finished_at`

### 6.4 审计日志

建议字段：

- `task_no`
- `step_code`
- `action_type`
- `action_result`
- `operator_type`
- `operator_id`
- `summary`
- `detail_json`
- `created_at`

## 7. 建议状态机

建议状态：

- `PENDING`
- `READY`
- `RUNNING`
- `WAITING_APPROVAL`
- `SUCCEEDED`
- `FAILED`
- `COMPENSATING`
- `COMPENSATED`
- `CANCELED`

建议主流转：

```text
PENDING -> READY -> RUNNING -> SUCCEEDED
PENDING -> READY -> RUNNING -> FAILED
FAILED -> READY
RUNNING -> WAITING_APPROVAL -> READY -> RUNNING
RUNNING -> COMPENSATING -> COMPENSATED
PENDING -> CANCELED
READY -> CANCELED
```

## 8. 优先级排序

### 8.1 第一优先级

- 发布任务执行
- 补偿任务处理
- 配置巡检

原因：

- 规则清晰
- 状态边界明确
- 已有设计资产较多
- 不依赖 AI 也能先落地

### 8.2 第二优先级

- 商家开通流程编排
- 运营批处理任务
- 数据巡检与异常发现

### 8.3 第三优先级

- AI 异常分析
- AI 参数建议
- AI 多步骤规划
- AI 半自动执行

## 9. 分阶段落地路线

### 9.1 第一阶段：任务执行底座

目标：

- 新增 `cdd-agent-core`
- 定义任务模型、状态机、执行器接口、审计接口
- 打通数据库持久化、状态推进、失败重试

验收标准：

- 能跑通一个无 AI 的标准任务
- 支持任务状态查询
- 支持失败重试和中文错误记录

### 9.2 第二阶段：补偿与发布场景接入

目标：

- 将补偿任务统一接到 Agent 执行底座
- 将发布任务的多步骤执行接入统一任务模型

验收标准：

- 发布任务与补偿任务都不再使用各自分散的执行骨架
- 有统一的任务列表、状态流转、审计记录

### 9.3 第三阶段：工具层标准化

目标：

- 抽象 `ToolInvoker`
- 沉淀 HTTP、MQ、内部服务调用工具

验收标准：

- 新任务不再直接在执行器中硬编码外部调用
- 工具调用日志可追踪

### 9.4 第四阶段：人工审批与人机协同

目标：

- 接入审批暂停、审批通过、审批驳回、人工重试

验收标准：

- 高风险任务可进入 `WAITING_APPROVAL`
- 审批动作有独立审计记录

### 9.5 第五阶段：AI 辅助能力

目标：

- 接入 AI 生成执行建议、异常分析、参数补全建议

验收标准：

- AI 先做建议，不默认直接执行
- 关键动作仍可配置为必须人工审批

## 10. 首批 Issue 拆解建议

### 10.1 Issue 1：搭建 `cdd-agent-core` 模块骨架

任务范围：

- 新增模块与 Maven 依赖
- 定义任务状态枚举、任务上下文、执行结果模型
- 定义执行器、审批网关、审计记录、工具调用接口

验收标准：

- Maven 编译通过
- 模块边界符合 `common-*` 约束
- 中文提示信息约束明确

当前落地（2026-03-21）：

- 已新增模块：`cdd-parent/cdd-agent-core`
- 已落地模型骨架：
  - 任务定义与实例：`TaskDefinition`、`TaskInstance`
  - 步骤定义与实例：`TaskStepDefinition`、`TaskStepInstance`
  - 状态机：`TaskStatus`、`TaskStepStatus`
  - 执行结果：`TaskExecutionResult`
  - 执行上下文：`TaskContext`
- 已落地接口骨架：
  - 执行器：`TaskExecutor`、`TaskStepExecutor`
  - 审计：`TaskAuditRecorder`
  - 审批：`TaskApprovalGateway`
  - 补偿：`TaskCompensationHandler`
- 本次明确不包含：
  - AI SDK 接入
  - 具体业务处理实现
  - 数据库迁移变更

### 10.2 Issue 2：任务表与审计表设计落地

任务范围：

- 新增任务主表、步骤表、审计表迁移脚本
- 统一状态字段、重试字段、上下文字段

验收标准：

- 数据库迁移可执行
- 表结构可支撑发布任务和补偿任务

### 10.3 Issue 3：执行器骨架与状态流转

任务范围：

- 实现任务创建、拉起、推进、失败、重试
- 实现统一中文错误收口

验收标准：

- 单元测试覆盖主状态流转
- 支持失败重试与超时终止

### 10.4 Issue 4：补偿任务接入 Agent 底座

任务范围：

- 将现有补偿任务模型接到统一执行器
- 保留 MQ 触发 + 定时扫描兜底

验收标准：

- 补偿任务不再绕开统一任务执行框架
- 重放与并发抢占链路可验证

### 10.5 Issue 5：发布任务接入 Agent 底座

任务范围：

- 将发布任务拆成标准步骤
- 接入审计与失败回滚

验收标准：

- 发布任务状态和步骤状态可查询
- 失败后可补偿或人工接管

### 10.6 Issue 6：工具层抽象

任务范围：

- 定义 `ToolInvoker`
- 落第一批内部工具适配器

验收标准：

- 至少支持内部服务调用、MQ 发送两类工具
- 工具输入输出可审计

## 11. 当前不建议立即做的内容

- 不立即做开放式多 Agent 协作
- 不立即做自动生成复杂业务流程
- 不立即把所有运营流程都接到 Agent
- 不立即让 AI 直接执行高风险动作
- 不立即引入重型工作流引擎替代全部业务流程

## 12. 当前推荐口径

- 当前项目：`业务系统骨架 + Agent 基础能力底座`
- 当前目标：先做任务执行与审计底座，再逐步接入 AI
- 当前最优先场景：发布任务、补偿任务、配置巡检
