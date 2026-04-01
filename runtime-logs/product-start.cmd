@echo off
cd /d F:\clauseWork\ChengDD
call "F:\clauseWork\ChengDD\runtime-logs\set-config-mode.cmd"
set CDD_LOCAL_MYSQL_ROOT_PASSWORD=root
set CDD_LOCAL_MYSQL_USERNAME=root
set CDD_PRODUCT_DB_USERNAME=root
set CDD_PRODUCT_DB_PASSWORD=root
"D:\workSoft\IntelliJ IDEA 2024.2.0.1\jbr\bin\java.exe" -jar "F:\clauseWork\ChengDD\cdd-parent\cdd-product-service\target\cdd-product-service-0.1.0-SNAPSHOT.jar" >> "F:\clauseWork\ChengDD\runtime-logs\product.out.log" 2>> "F:\clauseWork\ChengDD\runtime-logs\product.err.log"
