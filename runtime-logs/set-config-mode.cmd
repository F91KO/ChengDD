@echo off
if not defined CDD_ENV set CDD_ENV=local
if not defined CDD_CONFIG_MODE set CDD_CONFIG_MODE=file

if /I "%CDD_CONFIG_MODE%"=="nacos" (
  if not defined CDD_NACOS_SERVER_ADDR set CDD_NACOS_SERVER_ADDR=127.0.0.1:8848
  if not defined CDD_NACOS_GROUP set CDD_NACOS_GROUP=CHENGDD
  if not defined CDD_NACOS_NAMESPACE (
    if /I "%CDD_ENV%"=="local" (
      set CDD_NACOS_NAMESPACE=
    ) else (
      set CDD_NACOS_NAMESPACE=chengdd-%CDD_ENV%
    )
  )
)
