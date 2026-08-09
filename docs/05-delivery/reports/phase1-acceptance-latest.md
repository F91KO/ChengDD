# 一期自动化测试报告

## 验收结论

- 验收完成时间：2026-08-09 17:10:15 +0800。
- 结论：通过。Spring Cloud Alibaba + Nacos 接入、file 模式回归、Nacos 模式全服务启动、Gateway 鉴权转发、静态地址回退和停服注销均达到本轮标准。
- 令牌处理：登录令牌只存在于临时 shell 变量；报告和命令输出仅保留 `token=[REDACTED]` 摘要。

## 版本与依赖收敛

执行命令：

```bash
mvn -f cdd-parent/pom.xml -DskipTests validate
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.boot.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.cloud.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests help:evaluate -Dexpression=spring.cloud.alibaba.version -q -DforceStdout
mvn -f cdd-parent/pom.xml -DskipTests dependency:tree -Dincludes=org.springframework.boot:*,org.springframework.cloud:*,com.alibaba.cloud:*,com.alibaba.nacos:nacos-client
```

结果：

- Maven reactor：30/30 模块成功。
- Spring Boot：`3.5.14`。
- Spring Cloud：`2025.0.3`。
- Spring Cloud Alibaba：`2025.0.0.0`。
- Alibaba Starter：`2025.0.0.0`；Nacos Client 只有 `3.0.3`，未出现 Nacos Client 2.x。

## file 模式回归

执行命令：

```bash
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml -pl cdd-merchant-service -am -Dtest=MerchantAccountApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
CDD_CONFIG_MODE=file mvn -f cdd-parent/pom.xml test
python3 scripts/validation/check_module_boundaries.py
bash -n scripts/local/*.sh scripts/nacos/*.sh scripts/testing/*.sh
```

结果：

- Merchant Mockito 使用 `mock-maker-subclass`，定向测试 1/1 通过；JDK 21 下未添加 Byte Buddy self-attach 参数。
- 全 reactor 30/30 模块成功；在完整命令结束后立即汇总的 Surefire 结果为 23 个测试套件、tests=101、failures=0、errors=0、skipped=0。
- 模块边界检查通过；全部本地、Nacos、测试 shell 脚本语法检查通过。
- 静态回退定向测试 `GatewayRouteResolverTest#shouldFallBackWhenNoInstanceExists` 1/1 通过，7 模块 reactor 成功。

## Nacos 模式本地端到端

执行命令：

```bash
export CDD_ENV=local
export CDD_CONFIG_MODE=nacos
bash scripts/local/run_all_services_mysql.sh
bash scripts/local/status_all_services.sh
```

基础设施与配置结果：

- MySQL `127.0.0.1:3306`、Redis `127.0.0.1:6379`、Nacos Client API `127.0.0.1:8848` 均健康；Nacos Console 使用 `127.0.0.1:18080`，版本 `3.0.3`。
- 发布 11 份 Nacos 配置：公共配置、Gateway 配置和 9 个服务配置。
- Liquibase 对现有 `chengdd` 数据库执行结果：Run=0、Previously run=30、Total=30。
- HTTP 健康检查 10/10；Nacos `CHENGDD` 组内以下应用均为 hosts=1：
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

本地验收执行器在一次性 PTY 退出时会清理后代进程，因此本轮在持久交互 PTY 中执行原始 `run_all_services_mysql.sh`，并从独立命令执行 status 和 HTTP 请求；PTY 保持期间 10/10 服务持续健康。这是验收运行器行为，不是服务启动脚本或产品缺陷。

## Gateway 鉴权转发烟测

所有请求均通过 `http://127.0.0.1:8080`。先执行商户登录，随后仅在 shell 变量中携带 Bearer token；保存结果如下：

| 请求 | HTTP | JSON `code` |
| --- | ---: | ---: |
| `POST /api/auth/merchant/login` | 200 | 0 |
| `GET /api/auth/me` | 200 | 0 |
| `GET /api/product/spu?merchant_id=1001&store_id=1001` | 200 | 0 |
| `GET /api/order/orders?merchant_id=1001&store_id=1001&user_id=1001` | 200 | 0 |
| `GET /api/report/merchant-dashboard/latest?merchant_id=1001&store_id=1001` | 200 | 0 |
| `GET /api/config/merchant/feature-switches?merchant_id=merchant_1001` | 200 | 0 |

访问令牌：`[REDACTED]`。

## 静态地址回退实测

1. 停止 Nacos 模式的 Gateway 与 auth-service，确认端口 `8080`、`8081` 无监听，且 Gateway 注册 hosts=0。
2. 在持久 PTY 中以 `CDD_CONFIG_MODE=file` 分别执行 `bash scripts/local/run_auth_service_mysql.sh` 和 `bash scripts/local/run_gateway.sh`。
3. 两个进程日志均确认激活 profiles 为 `local,file`；Gateway 的 auth 静态基址为 `http://127.0.0.1:8081`。
4. 经 Gateway 请求 `POST /api/auth/merchant/login`，结果 HTTP 200、JSON `code=0`、`token=[REDACTED]`。
5. Gateway file-mode 日志扫描未出现 `NacosConfigDataLoader`、`NacosServiceRegistry`、`REGISTER-SERVICE` 或 Nacos discovery 活动。

结论：下游和 Gateway 在 file 模式重启后，Gateway 通过静态地址成功转发，未查询服务发现。

## 停服与注销

执行命令：

```bash
CDD_ENV=local CDD_CONFIG_MODE=nacos bash scripts/local/stop_all_services.sh
CDD_ENV=local CDD_CONFIG_MODE=nacos bash scripts/local/status_all_services.sh
```

结果：

- 全部 10 个受运行时状态文件保护的服务进程均被安全停止，HTTP healthy services=0/10。
- `cdd-gateway`、`cdd-auth-service`、`cdd-merchant-service`、`cdd-decoration-service`、`cdd-product-service`、`cdd-order-service`、`cdd-marketing-service`、`cdd-release-service`、`cdd-report-service`、`cdd-config-service` 均显示 `deregistered`。
- `lsof` 逐端口复核 `8080`–`8089`，10 个端口均无 LISTEN 进程。
- MySQL、Redis、Nacos 基础设施按验收要求保持运行，便于后续开发。

## 失败项与已解决问题

- 本轮最终验收无失败项。
- 验收过程中发现并先后独立修复、复核：Maven dependency tree 插件版本不稳定、订单测试同 JVM 顺序隔离、Liquibase 4.31.1 classpath changelog 解析兼容。修复后才恢复后续验收。

## 剩余生产风险

1. 当前 Nacos 为本地 standalone；生产需要独立评估集群高可用、持久化、TLS、认证、权限和密钥轮换。
2. 当前 MySQL、Redis 为本地单节点，并使用开发默认凭据；不可直接复制到生产。
3. Merchant 已验证 subclass mock maker；其他仍使用 Mockito inline 的测试模块在 JDK 21 可能继续输出 self-attach 提示，后续升级 JDK/Mockito 时需统一治理。
4. 当前烟测覆盖 Gateway 基础鉴权与同步转发，尚未覆盖消息链路、异步补偿、故障注入、限流熔断和多节点滚动升级。
