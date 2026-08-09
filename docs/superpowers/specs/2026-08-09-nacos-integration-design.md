# Nacos 配置中心与服务发现收口设计

## 1. 背景

项目已有 Nacos 配置加载基线，当前工作区还包含服务注册、Gateway 动态发现和本地全服务启停脚本的未提交实现。本轮不重新选型，而是延续现有 Nacos Java Client 方案，将代码、配置、测试和本地联调收口到一致状态。

本轮目标是完成代码层 Nacos 接入及本地 Docker Nacos 端到端联调。生产服务器 Compose、Nacos 生产鉴权和高可用集群继续作为后续部署任务。

## 2. 设计决策

采用轻量直连方案：

- 使用 `nacos-client 2.3.2`。
- 不引入 Spring Cloud Alibaba 依赖和版本矩阵。
- `cdd-common-core` 统一承载运行时参数解析、配置加载和服务注册。
- `cdd-gateway` 使用相同的客户端参数规则发现下游实例。
- 保留静态 `base-url` 作为 Gateway 的最后回退地址。
- `cdd-db-migration` 保持文件配置，不参与 Nacos 加载或服务注册。

## 3. 配置隔离与命名

### 3.1 环境隔离

| 环境 | Namespace |
| --- | --- |
| `local` | 默认公共 namespace |
| `dev` | `chengdd-dev` |
| `test` | `chengdd-test` |
| `prod` | `chengdd-prod` |

所有环境默认使用 `group=CHENGDD`。Namespace 和 Group 均允许通过环境变量覆盖。

### 3.2 公共配置

每个环境保留一份公共配置：

```text
cdd-common-{env}.yaml
```

公共配置仅承载可被多个服务共享的参数，例如 MySQL、Redis、JWT、日志和通用超时基线。不在公共配置中放入单个业务域的规则。

### 3.3 服务独立配置

每个可启动服务都有独立 DataId：

```text
{spring.application.name}-{env}.yaml
```

`prod` 环境的标准 DataId 为：

```text
cdd-common-prod.yaml
cdd-gateway-prod.yaml
cdd-auth-service-prod.yaml
cdd-merchant-service-prod.yaml
cdd-decoration-service-prod.yaml
cdd-product-service-prod.yaml
cdd-order-service-prod.yaml
cdd-marketing-service-prod.yaml
cdd-release-service-prod.yaml
cdd-report-service-prod.yaml
cdd-config-service-prod.yaml
```

服务独立配置承载端口、领域开关、线程池、路由和单服务超时等参数。

### 3.4 加载顺序

服务启动时按以下顺序处理：

1. Spring Boot 启动参数和操作系统环境变量。
2. Nacos 共享配置 `cdd-common-{env}.yaml`。
3. Nacos 服务配置 `{service-name}-{env}.yaml`。
4. 仓库内的基础 `application*.yaml` 保留启动必需默认值和静态回退值。

优先级必须保证：服务独立 Nacos 配置可覆盖共享 Nacos 配置；命令行参数和环境变量仍可覆盖 Nacos 值。

## 4. 组件边界

### 4.1 `NacosRuntimeSupport`

统一处理：

- `cdd.runtime.config-mode` / `CDD_CONFIG_MODE`
- `cdd.runtime.env` / `CDD_ENV`
- server address 规范化
- namespace、config group 和 discovery group
- 用户名、密码与 Nacos Client Properties
- fail-fast、必需配置和服务发现开关
- 共享 DataId 和服务 DataId 命名

Gateway 不应再自行复制 server address、namespace 和鉴权参数的解析逻辑。实施时将公共构建能力以有限、可测试的 API 暴露给 Gateway。

### 4.2 `NacosConfigEnvironmentPostProcessor`

- 仅在 `config-mode=nacos` 时运行。
- 在 Spring Bean 创建前加载远程 YAML。
- 先加载公共配置，再加载服务配置。
- 根据必需开关区分“缺失则跳过”与“缺失则启动失败”。
- 不为 `cdd-db-migration` 加载 Nacos 配置。

### 4.3 `NacosServiceRegistrationListener`

- 仅在 `config-mode=nacos` 且 discovery enabled 时运行。
- 收到 `ApplicationReadyEvent` 后注册服务实例。
- 收到 `ContextClosedEvent` 后注销实例并关闭客户端。
- 注册元数据包含运行环境、配置模式、context path 和管理端口。
- `local` 环境默认注册 `127.0.0.1`，其他环境允许通过参数显式指定 IP。
- 注册过程必须幂等，避免重复事件造成重复注册。

### 4.4 `GatewayRouteResolver`

- `file` 模式直接返回静态 `base-url`。
- `nacos` 模式按 `service-name` 和 discovery group 选择健康实例。
- Nacos Client 惰性创建并复用，应用关闭时释放。
- 暂时无健康实例或 Nacos 请求异常时，记录可定位的告警后回退到 `base-url`。
- 不改变 Gateway 现有的鉴权、权限和请求转发语义。

## 5. 运行时数据流

### 5.1 启动配置流

```text
启动命令
  -> 解析 env / config-mode
  -> 连接 Nacos
  -> 加载 cdd-common-{env}.yaml
  -> 加载 {service-name}-{env}.yaml
  -> 创建 Spring ApplicationContext
  -> 服务就绪
  -> 注册 Nacos 实例
```

### 5.2 Gateway 转发流

```text
前端请求
  -> cdd-gateway 鉴权/授权
  -> 根据路由获取 service-name
  -> Nacos 选择健康实例
  -> 转发下游
  -> 发现失败时回退 base-url
```

## 6. 容错规则

| 场景 | 行为 |
| --- | --- |
| `config-mode=file` | 不创建 Nacos Config/Naming Client |
| 非必需的 Nacos 配置缺失 | 跳过该 DataId，继续启动 |
| 必需配置缺失 | 抛出明确异常，阻止启动 |
| Nacos 连接失败且 fail-fast 开启 | 阻止启动 |
| 服务注册失败且 discovery fail-fast 开启 | 阻止服务完成启动 |
| Gateway 发现失败 | 记录告警，回退静态 `base-url` |
| 应用关闭时注销失败 | 记录告警，继续完成关闭 |

本地全服务启动脚本默认开启共享配置必需、服务配置必需、配置 fail-fast 和服务发现，使环境漂移在启动阶段直接暴露。

## 7. 测试设计

### 7.1 单元测试

需要覆盖：

- Nacos server address 的协议、`/nacos` 和尾斜杠规范化。
- Namespace、Group、DataId 和环境默认值。
- 共享配置与服务配置的覆盖顺序。
- 必需配置缺失时的 fail-fast。
- `file` 模式不访问 Nacos。
- 服务注册、重复事件幂等、关闭注销和客户端释放。
- Gateway 健康实例选择、`file` 模式和静态回退。

当前 JDK 21 环境中 Mockito inline mock maker 无法自附加。测试实现应优先使用不需要 Java Agent 的 mock maker 或轻量 fake，不将开放 JVM attach 当作项目运行前提。

### 7.2 构建与静态校验

- Nacos 相关模块定向测试。
- Maven 全量编译和项目边界检查。
- `git diff --check`。
- Shell 脚本语法校验。

### 7.3 本地端到端验收

1. 启动 MySQL、Redis 和 Nacos。
2. 发布 `local` 共享配置及所有服务配置。
3. 执行数据库迁移。
4. 以 `CDD_CONFIG_MODE=nacos` 启动 Gateway 和所有业务服务。
5. 检查 Nacos 配置列表与 `CHENGDD` 服务列表。
6. 通过 Gateway 验证登录、当前身份、商品、订单、报表和配置中心链路。
7. 停止服务，验证实例注销和停止脚本。

## 8. 文档与脚本交付

实施完成后同步：

- 根目录 `README.md`
- Nacos 命名与加载约定
- Nacos 配置导入与服务加载说明
- 本地数据库与 Nacos 启动说明
- 骨架验证和前端验收说明
- 全服务启动、状态检查和停止脚本

文档必须区分“本地已验证”和“生产待实施”，不把本轮本地联调写成服务器部署完成。

## 9. 非目标

本轮不包含：

- Spring Cloud Alibaba 迁移
- Nacos 生产鉴权与多节点集群
- 生产 Docker Compose 或 Kubernetes 清单
- Nginx、HTTPS 或 CI/CD
- 业务功能扩展
- 数据库迁移模块接入 Nacos

## 10. 完成标准

同时满足以下条件时，Nacos 接入才可标记完成：

- 公共配置与所有服务独立配置可正常加载。
- 服务独立配置可覆盖公共配置。
- Gateway 与业务服务都注册到 `CHENGDD`。
- Gateway 可通过 Nacos 健康实例完成真实请求转发。
- 必需配置、注册 fail-fast 和 Gateway 回退策略均有自动化测试。
- Nacos 相关单元测试、后端编译、模块边界检查和 Shell 语法检查通过。
- 本地全服务启动、状态检查、Gateway 冒烟和服务停止验证通过。
- 文档与实际配置键、DataId、Group 和启动命令一致。
