# 一期自动化测试报告

## 测试范围
- 一期核心链路：认证、网关、商户、商品、订单、发布、配置与通用权限校验。
- 验证脚本：scripts/validation/check_module_boundaries.py、scripts/validation/validate_backend_skeleton.sh。

## 执行时间
- 开始时间：2026-03-22 01:12:08 +0800
- 结束时间：2026-03-22 01:13:02 +0800

## 执行命令
- `bash scripts/validation/validate_backend_skeleton.sh`
- `MAVEN_OPTS=' -Djdk.attach.allowAttachSelf=true' mvn -q -s /var/folders/_f/dpr5gr191p5d564mykw_lpr80000gn/T//chengdd-mvn-settings.o4jWMx -Dmaven.repo.local=/Volumes/workspace/ChengDD/.m2/repository -f cdd-parent/pom.xml -pl cdd-common-security,cdd-auth-service,cdd-gateway,cdd-merchant-service,cdd-product-service,cdd-order-service,cdd-release-service,cdd-config-service -am test`

## 通过项
- cdd-common-security: tests=8 failures=0 errors=0 skipped=0
- cdd-auth-service: tests=1 failures=0 errors=0 skipped=0
- cdd-gateway: tests=3 failures=0 errors=0 skipped=0
- cdd-merchant-service: tests=5 failures=0 errors=0 skipped=0
- cdd-product-service: tests=2 failures=0 errors=0 skipped=0
- cdd-order-service: tests=4 failures=0 errors=0 skipped=0
- cdd-release-service: tests=4 failures=0 errors=0 skipped=0
- cdd-config-service: tests=3 failures=0 errors=0 skipped=0

## 失败项与失败原因
- 无。

## 关键日志或错误摘要
- 无。

## 是否达到当前任务验收标准
- 是
- 汇总：tests=30, failures=0, errors=0, skipped=0

## 未执行项与风险
- 无。
