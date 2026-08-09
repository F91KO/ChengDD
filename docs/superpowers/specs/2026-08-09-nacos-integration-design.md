# Spring Cloud Alibaba Nacos 接入收口设计

## 1. 背景

项目当前使用 Spring Boot 3.3.2，并在工作区中存在一组直接调用 `nacos-client` 的未提交实现，包括自定义配置加载、服务注册监听和 Gateway 实例解析。

本轮改用 Spring Cloud Alibaba 官方集成，不保留自研 Nacos 生命周期实现。同时将 Spring Boot 和 Spring Cloud 升级到 Spring Cloud Alibaba 2025.0 支持的版本线。

本轮目标是完成代码层 Nacos 配置中心、服务注册发现和 Gateway 负载均衡接入，并通过本地 Docker Nacos 3.0.3 完成端到端验收。生产 Compose、Nacos 生产鉴权和高可用集群仍属于后续部署任务。

## 2. 版本基线

| 组件 | 目标版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 保持现有基线 |
| Spring Boot | 3.5.14 | 3.5.x 当前稳定补丁版 |
| Spring Cloud | 2025.0.3 | 与 Spring Boot 3.5.x 对应的稳定发布列车 |
| Spring Cloud Alibaba | 2025.0.0.0 | 官方对应 Spring Boot 3.5.x / Spring Cloud 2025.0.x |
| Nacos Client | 3.0.3 | 由 Spring Cloud Alibaba BOM 管理 |
| Nacos Server | 3.0.3 | 与客户端基线保持一致 |

父 POM 统一导入：

- `spring-boot-dependencies`
- `spring-cloud-dependencies`
- `spring-cloud-alibaba-dependencies`

实施后必须执行 Maven dependency tree 和 Enforcer 收敛检查，确认没有混入 Spring Cloud 2024.x、Nacos Client 2.x 或旧版 Spring Framework。

## 3. 设计决策

- 配置中心使用 `spring-cloud-starter-alibaba-nacos-config`。
- 服务注册发现使用 `spring-cloud-starter-alibaba-nacos-discovery`。
- 配置导入统一使用 `spring.config.import`，不使用 `bootstrap.yaml`、`shared-configs` 或 `extension-configs`。
- Gateway 使用 Spring Cloud LoadBalancer 选择服务实例，不直接创建 Nacos `NamingService`。
- 删除自定义 Nacos `EnvironmentPostProcessor`、服务注册监听器和 Nacos Client 工厂逻辑。
- `cdd-db-migration` 继续使用文件配置，不引入 Nacos Starter，也不注册服务。
- 本轮只验收启动时配置加载，不把 Nacos 动态刷新作为完成条件。

## 4. 模块边界

### 4.1 `cdd-common-core`

- 保留通用环境、命名和运行时模型。
- 移除 `nacos-client` 直接依赖。
- 移除自定义 Nacos 配置加载与注册逻辑。
- 不承担 Spring Cloud Alibaba 适配职责。

### 4.2 `cdd-common-nacos`

新增独立适配模块，用于隔离 Spring Cloud Alibaba 依赖：

- 依赖 Nacos Config Starter 和 Discovery Starter。
- 提供项目统一的 Nacos 配置属性校验。
- 提供 `nacos` Spring Profile 下的共享配置导入约定。
- 不包含任何商品、订单或商户业务逻辑。

所有业务服务和 Gateway 依赖该模块；`cdd-db-migration`、`cdd-api-*`、`cdd-pay-core` 和 `cdd-agent-core` 不依赖该模块。

### 4.3 `cdd-gateway`

- 引入 `spring-cloud-starter-loadbalancer`。
- `GatewayRouteResolver` 仅依赖 Spring Cloud `LoadBalancerClient` 或等价抽象。
- 根据 `service-name` 选择实例，将 `ServiceInstance` 转换为下游基础 URL。
- 无可用实例时保留现有静态 `base-url` 回退。
- 不将现有 Spring MVC 代理强制改写为 Spring Cloud Gateway WebFlux，避免扩大本轮范围。

## 5. 配置隔离与命名

### 5.1 环境隔离

| 环境 | Namespace |
| --- | --- |
| `local` | Nacos 3.x public namespace |
| `dev` | `chengdd-dev` |
| `test` | `chengdd-test` |
| `prod` | `chengdd-prod` |

所有环境默认使用 `group=CHENGDD`。Namespace 实际传递值必须使用 Nacos 控制台显示的 Namespace ID，不依赖展示名称。

### 5.2 公共与独立配置

共享 DataId：

```text
cdd-common-{env}.yaml
```

服务独立 DataId：

```text
{spring.application.name}-{env}.yaml
```

`prod` 环境包含：

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

公共配置放 MySQL、Redis、JWT、日志和通用超时基线；服务独立配置放端口、领域开关、线程池、Gateway 路由和单服务超时。

### 5.3 `spring.config.import`

Nacos Profile 下必须使用非 optional 导入，配置缺失直接导致启动失败：

```yaml
spring:
  config:
    import:
      - nacos:cdd-common-${CDD_ENV:local}.yaml?group=CHENGDD&refreshEnabled=false
      - nacos:${spring.application.name}-${CDD_ENV:local}.yaml?group=CHENGDD&refreshEnabled=false
```

公共配置先导入，服务独立配置后导入，因此服务配置可覆盖公共值。命令行参数、系统属性和操作系统环境变量仍保持更高优先级。

### 5.4 Profile 与兼容模式

- Nacos 模式启动 Profile：`{env},nacos`。
- `nacos` Profile 负责开启 Config Import 和 Discovery。
- 仅启用 `{env}` Profile 时，允许使用仓库文件配置，并显式设置：

  ```yaml
  spring:
    cloud:
      nacos:
        config:
          enabled: false
          import-check:
            enabled: false
        discovery:
          enabled: false
  ```

- `nacos` Profile 显式将 Config、Config Import Check 和 Discovery 设为启用，并提供两个非 optional Config Import。
- 现有 `CDD_CONFIG_MODE` 在过渡期保留，启动脚本将其映射到 Spring Profile 和官方 enabled 配置，业务代码不再根据该字段创建 Nacos Client。

## 6. 运行时数据流

### 6.1 启动配置流

```text
启动脚本激活 {env},nacos
  -> Spring Config Data 处理 spring.config.import
  -> 加载 cdd-common-{env}.yaml
  -> 加载 {service-name}-{env}.yaml
  -> 创建 ApplicationContext
  -> Spring Cloud Alibaba 注册服务实例
```

### 6.2 Gateway 转发流

```text
前端请求
  -> cdd-gateway 鉴权/授权
  -> GatewayRouteResolver 获取 service-name
  -> Spring Cloud LoadBalancer 选择 ServiceInstance
  -> 构造下游 URL 并转发
  -> 无实例时记录告警并回退 base-url
```

## 7. 容错规则

| 场景 | 行为 |
| --- | --- |
| 未激活 `nacos` Profile | 禁用 Nacos Config/Discovery，使用文件配置 |
| Nacos Profile 缺少共享或服务配置 | 非 optional import 使启动失败 |
| Nacos Server 不可达 | 配置导入阶段快速失败 |
| 服务注册失败 | 启动验收失败，记录官方 Discovery 错误 |
| Gateway 无可用实例 | 记录告警，回退静态 `base-url` |
| 应用关闭 | 由 Spring Cloud Alibaba 完成实例注销和客户端释放 |

Nacos Config 和 Discovery HealthIndicator 默认保持关闭。端到端验收单独查询 Nacos 状态和服务列表，避免将 Nacos 短暂抖动直接等同于业务进程不存活。

## 8. 动态刷新边界

Spring Cloud Alibaba 2025.0.0.0 在较新 Spring Boot 3.5 补丁版上存在已报告的 `spring.config.import` 动态刷新风险。当前项目的旧实现也只支持启动时加载，因此本轮明确：

- Nacos 配置导入使用 `refreshEnabled=false`。
- 配置变更后通过受控重启服务生效。
- 动态刷新在 Spring Cloud Alibaba 后续补丁版发布后单独评估。
- 不使用自研监听器绕过官方刷新机制。

## 9. 测试设计

### 9.1 版本升级验证

- Maven Reactor 全量 `validate` / `compile` / `test`。
- 输出 Spring Boot、Spring Cloud、Spring Cloud Alibaba、Nacos Client 的 dependency tree。
- 检查重复类、版本冲突和过时 API。
- 执行现有认证、权限、商品、订单和 Gateway 回归测试。

### 9.2 Nacos 集成测试

需要覆盖：

- `file` 模式不导入 Nacos、不注册服务。
- Nacos 模式公共配置与服务配置加载顺序。
- 服务独立配置覆盖公共配置。
- 缺少必需 DataId 时启动失败。
- Spring Cloud Discovery 可看到已注册实例。
- LoadBalancer 可选择健康实例。
- Gateway 无实例时回退静态 URL。

单元测试不依赖 Mockito inline Java Agent，优先使用 Spring Cloud 抽象的 fake 或非 inline mock maker。

### 9.3 本地端到端验收

1. 将本地 Nacos 容器升级到 3.0.3，启动 MySQL、Redis 和 Nacos。
2. 发布 `local` 共享配置及所有服务配置。
3. 执行数据库迁移。
4. 激活 `local,nacos` 启动 Gateway 和所有业务服务。
5. 检查 Nacos 配置列表与 `CHENGDD` 服务列表。
6. 通过 Gateway 验证登录、当前身份、商品、订单、报表和配置中心链路。
7. 停止服务，验证实例注销和停止脚本。

## 10. 文档与脚本交付

实施完成后同步：

- 根目录 `README.md`
- Nacos 命名与加载约定
- Nacos 配置导入与服务加载说明
- 本地数据库与 Nacos 启动说明
- 骨架验证和前端验收说明
- 全服务启动、状态检查和停止脚本

文档必须区分“本地已验证”和“生产待实施”，不把本轮联调写成生产服务器部署完成。

## 11. 非目标

本轮不包含：

- Spring Cloud Gateway WebFlux 重写
- Nacos 配置动态刷新
- Nacos 生产鉴权与多节点集群
- 生产 Docker Compose 或 Kubernetes 清单
- Nginx、HTTPS 或 CI/CD
- 业务功能扩展
- 数据库迁移模块接入 Nacos

## 12. 完成标准

同时满足以下条件时，Nacos 接入才可标记完成：

- Maven 最终解析版本与第 2 节基线一致，不存在 Nacos Client 2.x 残留。
- 自定义 Nacos 配置加载器、注册监听器和直连 Naming Client 已移除。
- 公共配置与所有服务独立配置可通过 `spring.config.import` 正常加载。
- 服务独立配置可覆盖公共配置。
- Gateway 与业务服务都由 Spring Cloud Alibaba 注册到 `CHENGDD`。
- Gateway 可通过 Spring Cloud LoadBalancer 完成真实请求转发。
- 必需配置失败、Profile 切换和 Gateway 回退均有自动化测试。
- 全量后端编译、回归测试、模块边界检查和 Shell 语法检查通过。
- 本地全服务启动、状态检查、Gateway 冒烟和服务停止验证通过。
- 文档与实际版本、配置键、DataId、Group 和启动命令一致。
