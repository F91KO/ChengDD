# Nacos 配置命名与加载约定

## 1. 适用范围与版本

本约定适用于 Gateway 和九个业务服务，不包含 `cdd-db-migration`。

| Component | Version |
| --- | --- |
| Spring Boot | 3.5.14 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Nacos Client | 3.0.3 |

`cdd-common-nacos` 是唯一的 Nacos 适配边界。它引入 Spring Cloud Alibaba Config/Discovery starters，并仅在 `nacos` profile 下校验应用名、服务地址、环境、Config/Discovery group 及 namespace。其他公共模块、API 模块和数据库迁移模块不得直接依赖 Nacos Client。

## 2. 环境、Profile 与 Namespace

- 环境值：`local`、`dev`、`test`、`prod`。
- 运行时 profile 必须为 `{env},file` 或 `{env},nacos`，例如 `local,file`、`dev,nacos`。
- `CDD_ENV` 设置环境；`CDD_CONFIG_MODE` 只接受 `file` 或 `nacos`。
- `local` 可使用空 namespace。
- 非本地环境使用独立 namespace ID，且 Config 与 Discovery 必须使用同一个非空 ID。推荐名称为 `chengdd-dev`、`chengdd-test`、`chengdd-prod`，实际值通过 `CDD_NACOS_NAMESPACE` 提供。
- Config 与 Discovery group 固定为 `CHENGDD`。

## 3. DataId

共享配置：

```text
cdd-common-{env}.yaml
```

服务配置：

```text
{spring.application.name}-{env}.yaml
```

例如 `cdd-common-local.yaml`、`cdd-gateway-local.yaml`、`cdd-order-service-prod.yaml`。发布脚本会发布 1 份共享配置和 10 份应用配置。

## 4. 加载顺序

每个可执行模块的 `application.yaml` 保存应用名、profile 选择和静态默认值。启用 `nacos` profile 后，`application-nacos.yaml` 使用 Spring Boot Config Data API 按顺序导入：

```yaml
spring:
  config:
    import:
      - "nacos:cdd-common-${CDD_ENV:local}.yaml?group=${CDD_NACOS_GROUP:CHENGDD}&refreshEnabled=false"
      - "nacos:${spring.application.name}-${CDD_ENV:local}.yaml?group=${CDD_NACOS_GROUP:CHENGDD}&refreshEnabled=false"
```

共享配置先导入，服务配置后导入，因此服务级同名属性优先。两项都是必需导入，缺失或 Nacos 不可用会让 `nacos` 模式启动失败；动态刷新固定关闭，配置变更后需要重启服务。

`file` 模式只加载 `application.yaml` 和 `application-{env}.yaml`，Nacos Config/Discovery 都保持关闭，不访问 Nacos。file 与 nacos 是显式选择，不在同一次启动中自动互相回退。

## 5. Gateway 路由

Gateway 为 auth、merchant、decoration、product、order、marketing、release、report、config 九个下游同时保存 `service-name` 和静态 `base-url`。

- `nacos` profile：通过 Spring Cloud LoadBalancer 按服务名选择实例。
- 找不到实例或发现调用异常：记录警告并使用静态 `base-url`。
- `file` profile：不查询服务发现，直接使用静态 `base-url`。

## 6. 数据库迁移例外

`cdd-db-migration` 必须保持文件模式，不接入配置中心，也不注册服务。Liquibase `4.31.1` 运行时从 classpath 读取迁移资产，因此构建会把 `config/db-migration/db.changelog-master.yaml` 和 `db/migration/V*.sql` 按原目录结构打包；已有数据库可继续识别已执行的 30 个 changeSet。

## 7. 验证口径

```bash
python3 scripts/validation/check_module_boundaries.py
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml test
bash scripts/testing/run_nacos_integration.sh
bash scripts/nacos/check_nacos_state.sh local
```

集成测试覆盖共享/服务配置优先级、缺失配置、Nacos 不可达、file 模式不访问 Nacos，以及测试应用关闭后的注销。
