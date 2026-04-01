# Nacos配置导入与服务加载说明

## 目标

- 服务在 `CDD_CONFIG_MODE=nacos` 时，从 Nacos 读取共享配置和服务配置。
- Nacos 中的 dataId 命名遵循现有约定：
  - `cdd-common-{env}.yaml`
  - `{service-name}-{env}.yaml`
- `local` 仍然默认走文件配置；`dev / test / prod` 可以切到 Nacos。

## 当前实现

- 公共加载器位于 [NacosConfigEnvironmentPostProcessor.java](/F:/clauseWork/ChengDD/cdd-parent/cdd-common-core/src/main/java/com/cdd/common/core/runtime/NacosConfigEnvironmentPostProcessor.java)。
- 所有依赖 `cdd-common-core` 的服务都会自动具备这套加载能力。
- 当 `cdd.runtime.config-mode=file` 时，不访问 Nacos。
- 当 `cdd.runtime.config-mode=nacos` 时，按下面顺序加载：
  1. `cdd-common-{env}.yaml`
  2. `{spring.application.name}-{env}.yaml`
- 共享配置先加载，服务配置后加载，所以服务配置会覆盖共享配置中的同名键。

## 默认参数

- `CDD_NACOS_SERVER_ADDR`
  - 默认 `127.0.0.1:8848`
- `CDD_NACOS_GROUP`
  - 默认 `CHENGDD`
- `CDD_NACOS_NAMESPACE`
  - 默认 `local` 为空，其他环境默认 `chengdd-{env}`
- `CDD_NACOS_USERNAME`
  - 默认空
- `CDD_NACOS_PASSWORD`
  - 默认空
- `cdd.nacos.fail-fast`
  - 默认 `prod=true`，其他环境默认 `false`

## 导入方式

Linux / macOS:

```bash
bash scripts/nacos/publish_nacos_configs.sh dev
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\nacos\publish_nacos_configs.ps1 -EnvName dev
```

脚本会发布：

- `config/nacos/cdd-common-{env}.yaml`
- 每个服务模块下的 `src/main/resources/application-{env}.yaml`

## 启动方式

示例：

```bash
export CDD_ENV=dev
export CDD_CONFIG_MODE=nacos
export CDD_NACOS_SERVER_ADDR=127.0.0.1:8848
bash scripts/local/run_gateway.sh
```

或 Windows:

```powershell
$env:CDD_ENV='dev'
$env:CDD_CONFIG_MODE='nacos'
$env:CDD_NACOS_SERVER_ADDR='127.0.0.1:8848'
```

然后再执行对应启动脚本。

## 范围说明

- 已接入：`cdd-gateway` 和所有业务服务模块。
- 不纳入：`cdd-db-migration` 仍以文件配置为主，不依赖 Nacos 可用性。

## runtime-logs 启动脚本

- `runtime-logs/*.cmd` 和 `runtime-logs/*.ps1` 现在都会先加载统一 helper：
  - `runtime-logs/set-config-mode.cmd`
  - `runtime-logs/set-config-mode.ps1`
- 默认值：
  - `CDD_ENV=local`
  - `CDD_CONFIG_MODE=file`
- 如果要直接走 Nacos，启动前覆盖下面这些环境变量即可。

```powershell
$env:CDD_ENV='dev'
$env:CDD_CONFIG_MODE='nacos'
$env:CDD_NACOS_SERVER_ADDR='127.0.0.1:8848'
```

```cmd
set CDD_ENV=dev
set CDD_CONFIG_MODE=nacos
set CDD_NACOS_SERVER_ADDR=127.0.0.1:8848
```
