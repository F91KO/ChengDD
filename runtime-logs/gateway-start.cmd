@echo off
cd /d F:\clauseWork\ChengDD
call "F:\clauseWork\ChengDD\runtime-logs\set-config-mode.cmd"
set CDD_LOCAL_MYSQL_ROOT_PASSWORD=root
set CDD_LOCAL_MYSQL_USERNAME=root
set CDD_GATEWAY_AUTH_BASE_URL=http://127.0.0.1:8081
set CDD_GATEWAY_REPORT_BASE_URL=http://127.0.0.1:8088
set CDD_GATEWAY_CONFIG_BASE_URL=http://127.0.0.1:8089
set CDD_GATEWAY_PRODUCT_BASE_URL=http://127.0.0.1:8084
set CDD_GATEWAY_ORDER_BASE_URL=http://127.0.0.1:8085
set CDD_GATEWAY_RELEASE_BASE_URL=http://127.0.0.1:8087
"D:\workSoft\IntelliJ IDEA 2024.2.0.1\jbr\bin\java.exe" -jar "F:\clauseWork\ChengDD\cdd-parent\cdd-gateway\target\cdd-gateway-0.1.0-SNAPSHOT.jar" >> "F:\clauseWork\ChengDD\runtime-logs\gateway.out.log" 2>> "F:\clauseWork\ChengDD\runtime-logs\gateway.err.log"
