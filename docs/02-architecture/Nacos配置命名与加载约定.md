# Nacos 配置命名与加载约定

## 1. 文档说明

- 文档用途：统一程哒哒后端接入 Nacos 前的配置命名、环境隔离、加载顺序和本地回退规则。
- 当前阶段：配置中心接入设计基线，不等同于已完成代码接入。
- 适用范围：`cdd-parent/` 下的网关、业务服务、配置服务，不包含数据库迁移工具模块。

## 2. 目标

- 保持本地 `application*.yaml` 和未来 Nacos 配置一一对应。
- 避免不同服务、不同环境对 `namespace / group / dataId` 各自定义。
- 保证 `local` 环境不依赖 Nacos 也能开发和启动。
- 保证 `dev / test / prod` 环境后续接入 Nacos 时，不需要重命名现有配置键。

## 3. 环境与范围

### 3.1 环境枚举

- 本地环境：`local`
- 开发环境：`dev`
- 测试环境：`test`
- 生产环境：`prod`

### 3.2 适用模块

- `cdd-gateway`
- `cdd-auth-service`
- `cdd-merchant-service`
- `cdd-decoration-service`
- `cdd-product-service`
- `cdd-order-service`
- `cdd-marketing-service`
- `cdd-release-service`
- `cdd-report-service`
- `cdd-config-service`

### 3.3 不纳入范围

- `cdd-db-migration`

说明：
- 数据库迁移执行属于工具任务，不应依赖配置中心可用性。
- `cdd-db-migration` 继续使用仓库级文件配置：`config/db-migration/application-db-migration.yml`

## 4. 命名约定

### 4.1 Namespace 规则

- 每个部署环境使用独立 namespace。
- `dev / test / prod` 不共享 namespace。
- `local` 默认不依赖 namespace。

推荐映射：

- `local`：无
- `dev`：`chengdd-dev`
- `test`：`chengdd-test`
- `prod`：`chengdd-prod`

### 4.2 Group 规则

- 默认统一使用单一业务组：`CHENGDD`
- 当前阶段不再额外拆 `COMMON`、`GATEWAY`、`ORDER` 等多 group
- 如后续确需拆 group，应先修改本规范，再改实现

### 4.3 DataId 规则

服务主配置采用：

```text
{service-name}-{env}.yaml
```

示例：

- `cdd-gateway-dev.yaml`
- `cdd-order-service-test.yaml`
- `cdd-config-service-prod.yaml`

共享配置采用：

```text
cdd-common-{env}.yaml
```

示例：

- `cdd-common-dev.yaml`
- `cdd-common-test.yaml`
- `cdd-common-prod.yaml`

## 5. 本地文件与 Nacos 映射

### 5.1 当前本地文件结构

每个可启动模块统一保留：

- `application.yaml`
- `application-local.yaml`
- `application-dev.yaml`
- `application-test.yaml`
- `application-prod.yaml`

### 5.2 映射关系

- `application.yaml`
  - 保留服务自身最小启动配置
  - 不直接映射为 Nacos dataId
- `application-{env}.yaml`
  - 与 Nacos 中 `{service-name}-{env}.yaml` 一一对应

### 5.3 共享配置映射

后续进入 Nacos 后，跨服务共用项统一收敛到：

- `cdd-common-dev.yaml`
- `cdd-common-test.yaml`
- `cdd-common-prod.yaml`

不在当前阶段继续回到仓库级共享 `yaml` 文件结构。

## 6. 加载顺序约定

### 6.1 local 环境

- 仅依赖模块内本地文件配置。
- 默认加载：
  - `application.yaml`
  - `application-local.yaml`
- 不要求 Nacos 可用。

### 6.2 dev / test / prod 环境

推荐目标顺序：

1. `application.yaml`
2. Nacos 共享配置：`cdd-common-{env}.yaml`
3. Nacos 服务配置：`{service-name}-{env}.yaml`
4. 本地同环境文件作为可选兜底：`application-{env}.yaml`

说明：
- 当前阶段先定规则，不在此文中定义具体 Spring Boot 配置加载写法。
- 若后续决定“生产环境禁止本地兜底”，需在接入实现前单独补充约束。

## 7. 配置键约定

- 自定义配置继续使用 `cdd.*` 前缀。
- 当前保留运行时键：
  - `cdd.runtime.env`
  - `cdd.runtime.config-mode`
- 接入 Nacos 后不改已有业务配置前缀，不新增第二套同义键。

推荐值：

- `cdd.runtime.env=local|dev|test|prod`
- `cdd.runtime.config-mode=file|nacos`

## 8. 回退与故障策略

- `local` 环境：文件配置是主路径，不依赖 Nacos。
- `dev / test` 环境：允许在 Nacos 不可用时回退到本地同环境配置文件。
- `prod` 环境：默认设计为优先 Nacos，是否允许文件兜底需在正式接入前单独确认。

当前默认建议：

- `prod` 不把“文件兜底”作为常态能力依赖
- `prod` 如需兜底，应明确最近稳定版本、缓存策略和回退审计机制

## 9. 验收口径

- 每个服务都能从名称直接推导出对应 dataId。
- 本地 `application-{env}.yaml` 与 Nacos dataId 可一一映射。
- `namespace / group / dataId` 规则在文档中唯一确定，不再口头约定。
- `cdd-db-migration` 明确不进入 Nacos 配置链路。
- 后续接入实现时，不允许因为 Nacos 再改一轮配置命名。

## 10. 当前默认决策

- `namespace` 按环境隔离。
- `group` 统一使用 `CHENGDD`。
- 服务配置 `dataId` 统一采用 `{service-name}-{env}.yaml`。
- 共享配置 `dataId` 统一采用 `cdd-common-{env}.yaml`。
- `local` 只走文件配置。
- `dev / test / prod` 设计为优先 Nacos。
- `cdd-db-migration` 不接入 Nacos。

