# 一期自动化测试报告

## 测试范围
- 一期核心链路：认证、网关、商户、商品、订单、发布、配置与通用权限校验。
- 验证脚本：scripts/validation/check_module_boundaries.py、scripts/validation/validate_backend_skeleton.sh。

## 执行时间
- 开始时间：2026-03-22 15:09:32 +0800
- 结束时间：2026-03-22 15:10:29 +0800

## 2026-03-23 增量验证
- 测试库准备：`bash scripts/testing/prepare_mysql_test_db.sh`
- 配置服务 MySQL 集成测试：
  `mvn -q -s /tmp/chengdd-mvn-settings.xml -Dmaven.repo.local=/Volumes/workspace/ChengDD/.m2/repository -Dsurefire.failIfNoSpecifiedTests=false -pl cdd-config-service -am -Dtest=ConfigControllerIntegrationTest test`
- 报表服务 MySQL 集成测试：
  `mvn -q -s /tmp/chengdd-mvn-settings.xml -Dmaven.repo.local=/Volumes/workspace/ChengDD/.m2/repository -Dsurefire.failIfNoSpecifiedTests=false -pl cdd-report-service -am -Dtest=ReportControllerIntegrationTest,ReportApplicationServiceTest test`
- 商品服务 MySQL 集成测试：
  `mvn -q -s /tmp/chengdd-mvn-settings.xml -Dmaven.repo.local=/Volumes/workspace/ChengDD/.m2/repository -Dsurefire.failIfNoSpecifiedTests=false -pl cdd-product-service -am -Dtest=ProductControllerIntegrationTest test`
- 结果：
  - `cdd-config-service` 通过，发布记录创建/详情/回滚链路已验证。
  - `cdd-report-service` 通过，`data-health` 与商家维度 `health` 链路已验证。
  - `cdd-product-service` 通过，修复了同商品重复编辑时因历史软删 SKU 唯一键冲突导致的 500。
- 本轮关键修复：
  - `scripts/testing/prepare_mysql_test_db.sh` 显式导出测试库 MySQL 环境，确保迁移稳定落到 `chengdd_test`。
  - `JdbcProductCatalogStore.updateProduct` 在重建 SKU/库存前清理历史软删记录，避免重复编辑冲突。

## 执行命令
- `bash scripts/validation/validate_backend_skeleton.sh`
- `source scripts/testing/prepare_mysql_test_db.sh`
- `MAVEN_OPTS=' -Djdk.attach.allowAttachSelf=true' mvn -q -s /var/folders/_f/dpr5gr191p5d564mykw_lpr80000gn/T//chengdd-mvn-settings.CQjNuB -Dmaven.repo.local=/Volumes/workspace/ChengDD/.m2/repository -f cdd-parent/pom.xml -pl cdd-common-security,cdd-auth-service,cdd-gateway,cdd-merchant-service,cdd-product-service,cdd-order-service,cdd-release-service,cdd-config-service -am test`

## 通过项
- cdd-common-security: tests=8 failures=0 errors=0 skipped=0
- cdd-auth-service: tests=2 failures=0 errors=0 skipped=0
- cdd-gateway: tests=3 failures=0 errors=0 skipped=0
- cdd-merchant-service: tests=5 failures=0 errors=0 skipped=0
- cdd-product-service: tests=3 failures=0 errors=0 skipped=0
- cdd-order-service: tests=6 failures=0 errors=0 skipped=0
- cdd-release-service: tests=4 failures=0 errors=0 skipped=0
- cdd-config-service: tests=3 failures=0 errors=0 skipped=0

## 失败项与失败原因
- 无。

## 关键日志或错误摘要
- 无。

## 是否达到当前任务验收标准
- 是
- 汇总：tests=34, failures=0, errors=0, skipped=0

## 未执行项与风险
- 本地基础设施现已补齐 `Redis`，`order-service` 与 `config-service` 在 `local` 环境下的 `/actuator/health` 已恢复 `UP`；后续若更换开发机，需先执行 `bash scripts/local/up_local_infra.sh` 拉起完整基础设施。
