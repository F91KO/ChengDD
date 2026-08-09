# 前期单机 Docker 部署决策

## 1. 文档状态

- 决策日期：2026-08-09
- 当前状态：方案已确认，尚未实施服务器部署
- 当前优先级：保留部署决策，优先继续业务功能开发
- 适用阶段：项目前期、访问量较低、服务器资源有限时

## 2. 决策摘要

项目前期采用“单台服务器 + Docker Compose”部署方案，不在前期引入 Kubernetes。

已确认的基线：

- 服务器推荐规格为 `4 核 8 GB`，磁盘不低于 `80 GB SSD`。
- 业务应用、前端、MySQL、Redis 和 Nacos 由 Docker Compose 统一编排。
- Nacos 是必需组件，前期采用开启鉴权的单节点模式。
- MySQL 同时承载业务库和 Nacos 数据库，但必须使用不同的数据库名称和独立账号权限。
- 对外只暴露 Nginx 的 HTTP/HTTPS 端口，数据库、Redis、Nacos 和业务服务仅在 Docker 内部网络通信。
- 当前不实施高可用集群，通过持久化、备份、健康检查和资源限制降低单机风险。

## 3. 本文档范围

本文档只固化前期部署方向与约束，不代表相关部署产物已实现。

当前不在范围内：

- 后端服务 `Dockerfile`
- 前端静态站点 `Dockerfile`
- 生产版 `docker-compose.yml`
- Nginx 反向代理与 HTTPS 配置
- CI/CD 镜像构建和发布流程
- Kubernetes 、高可用 Nacos 和多节点部署
- 服务器自动化安装脚本

上述内容在准备服务器交付时单独建立实施任务，当前优先继续业务功能开发。

## 4. 前期部署拓扑

```text
公网用户
    |
    v
Nginx :80/:443
    |-- 前端静态资源
    `-- cdd-gateway
            |
            |-- cdd-auth-service
            |-- cdd-merchant-service
            |-- cdd-decoration-service
            |-- cdd-product-service
            |-- cdd-order-service
            |-- cdd-marketing-service
            |-- cdd-release-service
            |-- cdd-report-service
            `-- cdd-config-service

Docker 内部网络
    |-- MySQL
    |     |-- chengdd
    |     `-- nacos_config
    |-- Redis
    `-- Nacos standalone
```

## 5. 资源基线

| 组件 | 前期建议内存限制 |
| --- | ---: |
| Nginx 与前端 | `64-128 MB` |
| `cdd-gateway` | `256 MB` |
| 认证、商户服务 | 每个 `256 MB` |
| 商品、订单服务 | 每个 `384 MB` |
| 其他业务服务 | 每个 `192-256 MB` |
| MySQL | `1-1.5 GB` |
| Redis | `128-256 MB` |
| Nacos | `384-512 MB` |

所有 Java 服务必须显式设置容器内存上限和 JVM 堆上限，避免单个进程挤占整机内存。最终数值应在服务器压测后调整，不把本表作为永久容量上限。

## 6. Nacos 约定

前期 Nacos 使用单节点模式，但仍须按服务器环境管理：

- 固定使用已验证版本，不使用浮动的 `latest` 标签。
- 开启访问鉴权，设置独立的 Token 和服务端身份参数。
- 配置和服务发现统一使用 `group=CHENGDD`。
- 环境使用 `prod`，服务配置 `dataId` 保持 `{service-name}-prod.yaml`。
- 共享配置使用 `cdd-common-prod.yaml`。
- 业务服务运行时使用 `CDD_CONFIG_MODE=nacos`。
- Nacos 数据使用独立的 `nacos_config` 数据库和账号，不与业务表共用 schema。
- 管理端口不直接暴露到公网，运维访问使用 SSH 隧道或受控内网。
- Nacos 数据库与关键配置纳入备份范围。

## 7. 网络与安全

服务器公网原则上只开放：

- `22` / SSH，并限制来源 IP
- `80` / HTTP，用于 HTTPS 跳转或证书签发
- `443` / HTTPS

不得直接暴露到公网：

- MySQL `3306`
- Redis `6379`
- Nacos `8848` / `9848` / `9849`
- Gateway 之外的业务服务端口 `8081-8089`

密码、JWT 密钥、Nacos Token 和第三方密钥必须通过服务器环境变量或密钥文件注入，不得写入 Git 仓库、镜像或日志。

## 8. 数据与备份

- MySQL、Redis 和 Nacos 数据目录必须挂载持久化 volume。
- MySQL 每日至少生成一次备份。
- 备份必须复制到当前服务器之外的存储位置。
- 业务库与 Nacos 库都纳入备份，并定期执行恢复演练。
- 数据库迁移统一由仓库的 Liquibase 迁移模块执行，不手工跳过版本脚本。

## 9. 建议启动顺序

后续实施生产 Compose 时，启动顺序固定为：

1. MySQL
2. Redis
3. Nacos
4. 数据库迁移
5. 认证与业务服务
6. Gateway
7. Nginx 与前端
8. 健康检查与基础业务冒烟

每一层必须等待上游健康后再启动，不仅依赖固定时间的 `sleep`。

## 10. 单机阶段风险接受

前期方案接受单机故障会导致整体服务中断的风险，但不接受以下可避免风险：

- 无持久化导致重启后数据丢失
- 无备份或备份只保存在同一台服务器
- Nacos、MySQL 或 Redis 直接暴露到公网
- 在仓库中保存真实密码和密钥
- 容器无资源上限，导致单个服务拖垮整机
- 没有健康检查、重启策略或日志轮转

## 11. 扩容路线

不以固定时间切换架构，而是在出现以下信号时逐步拆分：

1. MySQL 持续占用较高或备份恢复时间无法接受：迁移到托管数据库或独立数据库主机。
2. Redis 开始承载大量会话、锁或高频缓存：迁移到托管 Redis 或独立节点。
3. Nacos 中断时间已不能接受：升级为多节点集群。
4. 应用服务需要多副本、灰度和弹性伸缩：再评估 Kubernetes。

长期生产方向参见《[生产环境 K8s 多商户部署建议](./生产环境K8s多商户部署建议.md)》。

## 12. 后续实施入口

当确定服务器规格、操作系统、域名和证书方案后，再建立独立部署任务，产出：

- 各应用服务的容器镜像定义
- 生产版 Docker Compose
- Nacos 数据库初始化与鉴权配置
- Nginx、HTTPS 和健康检查
- 备份、恢复、升级与回滚脚本
- 服务器交付验收清单

本阶段不执行上述部署实施，项目继续优先推进业务功能。
