# 开发与测试 Sub-Agent 执行规范

## 1. 目标

- 统一“主 agent 负责任务拆解与验收，sub-agent 负责编码与测试”的协作方式。
- 默认采用“主 agent 拆解任务，sub-agent 按服务并行执行”的模式推进开发。
- 让编码执行、测试验证、失败分析、测试报告输出形成固定流程，避免每次临时组织。
- 保证测试阶段提示信息、报告、结论统一使用中文。

## 2. 适用范围

- 本仓库内的任务拆解、代码实现、模块测试、一期验收、骨架验证、回归验证。
- 适用于“主 agent 负责任务管理与验收，sub-agent 负责执行”的协作模式。

## 3. 角色分工

### 3.1 主 agent

- 负责任务拆解、范围控制、验收标准定义、最终结论。
- 负责决定是否启动编码 sub-agent、测试 sub-agent、失败分析 sub-agent。
- 负责阅读编码结果与测试报告，并决定是否进入修复、提交、PR 或合并。

### 3.2 编码 sub-agent

- 默认只负责实现指定范围内的代码。
- 默认不负责最终验收结论。
- 完成后应输出本次改动范围、关键实现点、需要回归验证的项。

### 3.3 测试 sub-agent

- 默认只负责执行测试、整理日志、输出测试报告。
- 默认不负责业务需求扩展与代码实现。
- 若发现失败，仅输出失败摘要、影响范围、复现命令、初步判断，不直接修复。

### 3.4 失败分析 sub-agent

- 在测试失败后启动。
- 负责定位失败原因、归纳根因、给出修复建议。
- 默认不直接提交代码，除非主流程明确授权其处理某个失败项。

## 4. 模型选择规则

### 4.1 快速验证

- 推荐模型：`gpt-5.1-codex-mini`
- 适用场景：
  - 执行单元测试
  - 执行集成测试
  - 执行启动验证
  - 执行验收脚本
  - 汇总 surefire 报告
  - 生成标准测试报告

说明：

- 本文中的“自动化测试”默认指单元测试、集成测试、启动验证、验收脚本中的适用组合，不等于只跑单元测试。

### 4.2 失败定位

- 推荐模型：`gpt-5.3-codex` 或 `gpt-5.4`
- 适用场景：
  - 编译失败分析
  - 测试失败根因定位
  - 多模块联动问题分析
  - 日志较长、链路较深的问题收敛

### 4.3 最终复核

- 推荐模型：`gpt-5.2` 或 `gpt-5.4`
- 适用场景：
  - 验收标准复核
  - 测试报告完整性检查
  - 是否达到提交/PR 条件的最终判断

### 4.4 并行协作补充规则

- 当同一轮任务需要并行启动多个 sub-agent 时，默认使用不同模型，不要把所有 sub-agent 固定在同一模型上。
- 推荐分配方式：
  - UI/页面收口：`gpt-5.4`
  - 服务接口、状态管理、复杂代码改造：`gpt-5.3-codex`
  - 文档整理、报告归档、快速验证：`gpt-5.1-codex-mini`
- 若某轮任务有特殊原因使用相同模型，主 agent 需要在任务说明里写明原因。

## 5. 标准执行流程

1. 主 agent 先输出本次任务 list、范围、依赖关系和验收标准。
2. 主 agent 按任务边界启动一个或多个编码 sub-agent；若写入范围不冲突，默认并行执行。
3. 编码 sub-agent 完成实现后，向主 agent 回传改动范围和待验证项。
4. 主 agent 收口本轮改动后，启动测试 sub-agent 执行验证。
5. 测试 sub-agent 按约定命令执行测试，并输出测试报告到固定路径。
6. 若测试失败，测试 sub-agent 先输出失败摘要。
7. 主 agent 决定是否启动失败分析 sub-agent 做根因定位。
8. 根因明确后，由主 agent 决定是否再次派发编码 sub-agent 修复。
9. 修复完成后，再次启动测试 sub-agent 回归验证。
10. 自动化测试通过后，继续执行本地环境验证。
11. 本地环境验证通过后，由主 agent 做最终验收结论，并决定是否进入提交、PR 或合并阶段。

## 6. 编码 sub-agent 约束

- 提示信息、结论统一使用中文。
- 只在主 agent 指定的任务边界内编码，不主动扩 scope。
- 默认不做最终验收判断。
- 完成后必须明确：
  - 修改了哪些模块或文件
  - 实现了哪些子任务
  - 哪些点需要测试 sub-agent 回归
- 若发现任务边界不清、接口不完整或依赖阻断，应先回报主 agent。

## 7. 测试 sub-agent 约束

- 提示词、日志结论、测试报告统一使用中文。
- 默认不修改业务代码。
- 默认不创建、删除、回滚分支。
- 默认不执行提交、推送、PR、issue 创建。
- 默认优先复用项目现有脚本，不自行发明新的测试入口。
- 若发现环境问题，应明确区分“环境失败”和“代码失败”。
- 若测试依赖外部服务，应在报告中标明依赖项是否可用。
- 若主流程要求提交前验收，测试 sub-agent 不能只停留在自动化测试通过，必须明确本地环境验证是否已执行、是否通过。

## 8. 推荐执行命令

### 7.1 一期验收

```bash
bash scripts/testing/run_phase1_acceptance.sh
```

若本轮仅验证代码，不做本地基础设施启动检查：

```bash
bash scripts/testing/run_phase1_acceptance.sh --skip-skeleton
```

### 7.2 骨架验证

```bash
bash scripts/validation/validate_backend_skeleton.sh
```

### 7.3 单模块测试

```bash
mvn -q -f cdd-parent/pom.xml -pl <module> -am test
```

### 7.4 本地环境验证

```bash
bash scripts/validation/validate_backend_skeleton.sh
```

如任务只影响单个服务，也应至少补充一轮对应服务的本地启动或关键链路验证。

## 9. 测试报告输出规范

默认输出位置：

- 最新报告：`docs/05-delivery/reports/phase1-acceptance-latest.md`
- 模板文件：`docs/05-delivery/测试报告模板.md`

测试报告至少包含：

- 测试范围
- 执行时间
- 执行命令
- 通过项
- 失败项与失败原因
- 关键日志或错误摘要
- 是否达到当前任务验收标准
- 未执行项与风险

若本轮准备提交、PR 或合并，还应额外说明：

- 本地环境验证是否已执行
- 本地环境验证覆盖了哪些依赖与链路
- 是否存在未验证的环境风险

## 10. 失败后的标准输出

测试 sub-agent 失败时，至少输出以下内容：

- 失败模块
- 失败类型：编译失败、测试失败、环境失败、权限失败
- 直接报错摘要
- 影响范围
- 复现命令
- 是否阻断当前提交

分析 sub-agent 输出时，至少包含：

- 根因判断
- 涉及模块/文件
- 修复建议
- 回归建议

## 11. 并发与写入约束

- 后续任务默认启用 sub-agent 并行，但前提是服务边界清晰、写入范围不冲突。
- 不允许多个测试 sub-agent 同时写同一份报告文件。
- 不允许多个编码 sub-agent 修改同一组核心文件，除非主 agent 明确做了写入边界划分。
- 若需要并行测试，应按模块拆分报告文件，再由主 agent 汇总。
- 并行场景下，一个 sub-agent 只负责一个清晰边界的编码范围或测试范围。

## 12. 权限与安全约束

- 编码 sub-agent 与测试 sub-agent 执行命令时，优先复用已有持久化前缀授权。
- 若遇到新的命令授权，先申请稳定、可复用、范围适中的前缀授权。
- sub-agent 不执行破坏性命令，例如：
  - `rm`
  - `git reset --hard`
  - `git checkout --`
- 若涉及本地环境启动，应在报告中写明使用了哪些端口、配置和依赖。

## 13. 按服务定义 Sub-Agent

建议后续编码 sub-agent 主要按服务边界定义，而不是按“写接口”“补测试”“改配置”这类临时动作定义。

命名规则建议：

- `<service>-coding-agent`
- `<service>-testing-agent`
- `<service>-analysis-agent`

例如：

- `order-service-coding-agent`
- `merchant-service-testing-agent`
- `gateway-analysis-agent`

### 13.1 服务级 agent 定义建议

| 服务/模块 | 推荐 agent 名称 | 主要职责 | 建议写入范围 | 推荐模型 | 常用验证命令 |
| --- | --- | --- | --- | --- | --- |
| `cdd-auth-service` | `auth-service-coding-agent` | 认证、登录、刷新、登出、令牌相关实现 | `cdd-parent/cdd-auth-service/**`、必要时 `cdd-parent/cdd-api-auth/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-auth-service -am test` |
| `cdd-gateway` | `gateway-coding-agent` | 网关上下文透传、鉴权接入、统一入口能力 | `cdd-parent/cdd-gateway/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-gateway -am test` |
| `cdd-merchant-service` | `merchant-service-coding-agent` | 商家入驻、审核、一键开通等能力 | `cdd-parent/cdd-merchant-service/**`、必要时 `cdd-parent/cdd-api-merchant/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-merchant-service -am test` |
| `cdd-product-service` | `product-service-coding-agent` | 分类模板、商家分类树、商品目录、库存能力 | `cdd-parent/cdd-product-service/**`、必要时 `cdd-parent/cdd-api-product/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-product-service -am test` |
| `cdd-order-service` | `order-service-coding-agent` | 购物车、结算、下单、支付、退款、订单流转 | `cdd-parent/cdd-order-service/**`、必要时 `cdd-parent/cdd-api-order/**`、`cdd-parent/cdd-api-pay/**` | `gpt-5.4` 优先 | `mvn -q -f cdd-parent/pom.xml -pl cdd-order-service -am test` |
| `cdd-release-service` | `release-service-coding-agent` | 小程序发布、版本映射、回滚、发布治理 | `cdd-parent/cdd-release-service/**`、必要时 `cdd-parent/cdd-api-release/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-release-service -am test` |
| `cdd-config-service` | `config-service-coding-agent` | 配置项、功能开关、商家覆盖能力 | `cdd-parent/cdd-config-service/**`、必要时 `cdd-parent/cdd-api-config/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-config-service -am test` |
| `cdd-common-security` | `common-security-coding-agent` | RBAC、数据范围、认证上下文、安全拦截器 | `cdd-parent/cdd-common-security/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-common-security -am test` |
| `cdd-agent-core` | `agent-core-coding-agent` | 任务定义、执行、补偿、审批、审计底座 | `cdd-parent/cdd-agent-core/**` | `gpt-5.3-codex` / `gpt-5.4` | `mvn -q -f cdd-parent/pom.xml -pl cdd-agent-core -am test` |

### 13.2 服务级 agent 使用原则

- 一个编码 sub-agent 默认只负责一个服务或一个公共模块。
- 跨服务任务由主 agent 拆成多个服务级 sub-agent 并行执行，再统一收口。
- 若确需跨服务修改，主 agent 必须提前明确主责服务与次级依赖服务。
- 服务级测试 agent 可以按服务运行模块测试，也可以由通用测试 agent 统一跑一期验收。
- 线上问题排查时，优先启动与问题服务对应的 `analysis-agent`，避免无边界全仓扫描。

## 14. 推荐提示词模板

### 14.1 编码执行 agent 提示词

可直接复用：

```text
你是当前仓库的编码 sub-agent。
你的职责是在主 agent 已经给定的任务边界内完成代码实现，不负责最终验收结论。
所有提示信息、结论统一使用中文。
不要主动扩展需求范围，不要修改未分配的模块。
完成后至少输出：
1. 本次实现的子任务
2. 涉及模块和文件
3. 关键实现点
4. 建议测试覆盖点
若遇到依赖缺失、接口未定、任务边界冲突，请先回报主 agent。
```

### 14.2 测试执行 agent 提示词

可直接复用：

```text
你是当前仓库的测试 sub-agent。
你的职责仅限于执行自动化测试、整理失败信息、输出测试报告，不修改业务代码。
所有提示信息、报告、结论统一使用中文。
优先复用仓库现有测试脚本与验证脚本。
输出内容至少包含：测试范围、执行命令、结果摘要、失败项与原因、是否达到当前任务验收标准。
报告输出到 docs/05-delivery/reports/phase1-acceptance-latest.md。
若测试失败，先给出失败摘要、影响范围和复现命令，不直接修复代码。
```

### 14.3 失败分析 agent 提示词

可直接复用：

```text
你是当前仓库的失败分析 sub-agent。
你的职责仅限于阅读测试日志、定位根因、整理修复建议，不直接修改业务代码，除非主流程明确授权。
所有提示信息、结论统一使用中文。
请先区分失败类型：编译失败、测试断言失败、环境失败、权限失败、配置失败。
输出内容至少包含：
1. 失败模块
2. 直接报错摘要
3. 根因判断
4. 涉及文件或模块
5. 修复建议
6. 回归建议
如果信息不足，请明确指出缺失信息，不要猜测为结论。
```

### 14.4 提交前复核 agent 提示词

可直接复用：

```text
你是当前仓库的提交前复核 sub-agent。
你的职责是根据已有测试报告、执行记录和当前任务验收标准，判断当前改动是否达到提交、PR 或合并条件。
所有提示信息、结论统一使用中文。
不要重新实现代码，重点做复核判断。
输出内容至少包含：
1. 本轮验证范围
2. 已执行验证项
3. 未执行项
4. 当前是否达到验收标准
5. 是否允许提交/PR
6. 剩余风险
若测试报告、验收标准或执行记录缺失，请直接指出阻断项。
```

## 15. 标准工作流示例

推荐按以下顺序执行：

1. 主 agent
- 输出任务 list。
- 明确本轮范围、依赖关系、验收标准。
- 决定需要几个按服务划分的编码 sub-agent，以及每个 sub-agent 的负责范围。

2. 编码 sub-agent
- 分别实现独立服务或独立子任务。
- 完成后回传改动点、关键实现、建议回归点。

3. 主 agent
- 收口各 sub-agent 的实现结果。
- 检查是否存在边界冲突、接口不一致、遗漏配置。
- 决定测试范围和测试命令。

4. 测试 sub-agent
- 执行模块测试、验收脚本或骨架验证。
- 输出统一测试报告。
- 若失败，输出失败摘要、影响范围和复现命令。

5. 失败分析 sub-agent
- 在测试失败时定位根因。
- 给出修复建议和回归建议。

6. 主 agent
- 判断是否需要再次派发编码 sub-agent 修复。
- 自动化测试与本地环境验证都通过后，做最终验收结论。
- 明确是否达到提交、PR 或合并条件。

### 15.1 一个典型案例

例如“订单支付回调 + 退款补偿 + 验收”的执行方式：

1. 主 agent
- 拆成三个子任务：
  - `order-service` 支付回调实现
  - `order-service` 退款补偿实现
  - 测试与验收

2. 编码 sub-agent
- 启动 `order-service-coding-agent`
- 负责目录：
  - `cdd-parent/cdd-order-service/**`
  - `cdd-parent/cdd-api-order/**`
  - `cdd-parent/cdd-api-pay/**`

3. 测试 sub-agent
- 先执行：
  - `mvn -q -f cdd-parent/pom.xml -pl cdd-order-service -am test`
- 再执行：
  - `bash scripts/testing/run_phase1_acceptance.sh --skip-skeleton`

4. 失败分析 sub-agent
- 若失败，启动 `order-service-analysis-agent`
- 只分析订单域相关失败，不扩散到其他服务

5. 主 agent
- 根据测试报告和本地环境验证结果做最终验收
- 判断是否允许提交或继续修复

## 16. 当前项目落地建议

- 默认由主 agent 负责任务拆解、issue/分支流程控制和最终验收。
- 默认使用 `gpt-5.3-codex`、`gpt-5.4` 或同等级模型作为编码 sub-agent。
- 默认使用 `gpt-5.1-codex-mini` 作为测试执行 sub-agent。
- 测试失败后，按需使用 `gpt-5.3-codex` 或 `gpt-5.4` 做失败分析。
- 当前仓库进入提交、PR、合并前，必须有对应测试报告。
- 当前仓库进入提交、PR、合并前，默认还必须有本地环境验证通过结论。
- 若使用 sub-agent 执行编码和测试，主 agent 必须在最终结论里引用任务 list、测试报告位置和验收结论。
- 服务级 agent 的独立清单见 [服务级Agent清单.md](./服务级Agent清单.md)。
