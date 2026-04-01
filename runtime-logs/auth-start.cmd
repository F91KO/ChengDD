@echo off
cd /d F:\clauseWork\ChengDD
call "F:\clauseWork\ChengDD\runtime-logs\set-config-mode.cmd"
set CDD_LOCAL_MYSQL_ROOT_PASSWORD=root
set CDD_LOCAL_MYSQL_USERNAME=root
set CDD_AUTH_DB_USERNAME=root
set CDD_AUTH_DB_PASSWORD=root
set CDD_AUTH_SQL_INIT_MODE=never
"D:\workSoft\IntelliJ IDEA 2024.2.0.1\jbr\bin\java.exe" -jar "F:\clauseWork\ChengDD\cdd-parent\cdd-auth-service\target\cdd-auth-service-0.1.0-SNAPSHOT.jar" >> "F:\clauseWork\ChengDD\runtime-logs\auth.out.log" 2>> "F:\clauseWork\ChengDD\runtime-logs\auth.err.log"
