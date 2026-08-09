# Nacos 配置导入与服务加载说明

## 当前实现

后端使用 Spring Boot `3.5.14`、Spring Cloud `2025.0.3`、Spring Cloud Alibaba `2025.0.0.0` 和 Nacos Client `3.0.3`。`cdd-common-nacos` 封装官方 Config/Discovery starters，Gateway 和九个业务服务只通过该模块接入；`cdd-db-migration` 保持文件模式。

运行模式由两个 profile 组成：

- `CDD_ENV=local|dev|test|prod`
- `CDD_CONFIG_MODE=file|nacos`

本地生命周期脚本的默认值是 `local,file`。`nacos` 模式使用官方属性 `spring.cloud.nacos.server-addr`、`spring.cloud.nacos.config.*`、`spring.cloud.nacos.discovery.*`；环境变量映射如下：

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `CDD_NACOS_SERVER_ADDR` | `127.0.0.1:8848` | Config/Discovery client address |
| `CDD_NACOS_GROUP` | `CHENGDD` | Config/Discovery group |
| `CDD_NACOS_NAMESPACE` | empty | local 可为空；非 local 必须显式设置 |
| `CDD_NACOS_USERNAME` / `CDD_NACOS_PASSWORD` | empty | 鉴权启用时成对提供 |

## 配置导入

十个应用的 `application-nacos.yaml` 按以下顺序导入，且固定 `refreshEnabled=false`：

1. `cdd-common-{env}.yaml`
2. `{spring.application.name}-{env}.yaml`

服务配置因此覆盖共享配置中的同名键。导入不是 optional；缺少任一 DataId 或 Nacos 不可用时，nacos 模式启动失败。file 模式不访问 Nacos，也不会自动切到 Nacos。

发布 local 配置：

```bash
bash scripts/nacos/publish_nacos_configs.sh local
```

发布非本地配置前必须提供对应 namespace ID：

```bash
export CDD_NACOS_NAMESPACE=chengdd-dev
bash scripts/nacos/publish_nacos_configs.sh dev
```

发布内容为 `config/nacos/cdd-common-{env}.yaml` 与十个模块的 `application-{env}.yaml`，共 11 个 DataId，group 为 `CHENGDD`。

## 启动、检查与停止

file 模式：

```bash
CDD_ENV=local CDD_CONFIG_MODE=file bash scripts/local/run_all_services_mysql.sh
```

Nacos 模式：

```bash
export CDD_ENV=local
export CDD_CONFIG_MODE=nacos
bash scripts/local/run_all_services_mysql.sh
bash scripts/local/status_all_services.sh
bash scripts/nacos/check_nacos_state.sh local
bash scripts/local/stop_all_services.sh
```

启动脚本会依次准备 MySQL、Redis、Nacos，发布配置，执行 Liquibase，再按 8080–8089 启动 Gateway 和九个服务。Nacos 模式的状态检查同时验证 HTTP 10/10 健康和 `CHENGDD` 组内 10 个服务均有实例；停止脚本只终止状态文件确认归属的进程，并等待全部实例注销。

Gateway 在 nacos 模式通过 Spring Cloud LoadBalancer 按服务名发现下游；无实例或发现异常时使用静态地址。file 模式始终使用静态地址，不查询 Nacos。

## 验证与已知问题

```bash
bash scripts/testing/run_nacos_integration.sh
```

该命令会创建唯一的临时 namespace，在其中发布契约配置，执行共享/服务优先级、缺失配置、不可达、file 模式和注销测试，最后删除临时 namespace。脚本会识别现有 Nacos 的实际 Console/Client 端口，并在退出时恢复容器原本的 running、stopped 或 absent 状态，不覆盖当前项目的 11 个 DataId。

2026-08-09 的本地验收中，10 个服务均完成注册、Gateway 转发、进程退出、端口清理和最终注销。10 个 Nacos 模式服务在成功注销后仍记录 `NacosGracefulShutdownDelegate` ERROR/NPE；该日志缺陷未影响功能性清理，但需要后续做版本兼容性验证。
