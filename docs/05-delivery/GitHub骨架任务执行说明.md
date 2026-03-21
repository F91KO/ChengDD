# GitHub 骨架任务执行说明

## 1. 说明

- 当前仓库内已准备好本地规划分支和 GitHub issue 创建脚本。
- 由于当前机器没有可用的 GitHub 认证，远端分支推送和 issue 创建未自动完成。
- 你完成 GitHub 认证后，可以直接按本文步骤执行。

## 2. 已准备好的内容

本地已创建分支：

- `feature-bootstrap-parent`
- `feature-common-modules`
- `feature-api-modules`
- `feature-db-migration-bootstrap`
- `feature-gateway-skeleton`
- `feature-auth-skeleton`
- `feature-merchant-skeleton`
- `feature-product-order-skeleton`
- `feature-release-config-skeleton`
- `feature-supporting-services-skeleton`
- `feature-skeleton-validation`

已新增脚本：

- [push_project_skeleton_branches.sh](/Volumes/workspace/ChengDD/scripts/github/push_project_skeleton_branches.sh)
- [create_project_skeleton_issues.py](/Volumes/workspace/ChengDD/scripts/github/create_project_skeleton_issues.py)

## 3. 先完成 GitHub 认证

任选一种方式：

1. 配置 `gh auth login`
2. 配置 GitHub HTTPS 凭据
3. 导出环境变量 `GITHUB_TOKEN`

脚本要求：

- 远端推分支需要 Git push 权限
- 创建 issue 需要 `GITHUB_TOKEN`

## 4. 推送规划分支

```bash
bash scripts/github/push_project_skeleton_branches.sh
```

可选参数：

```bash
bash scripts/github/push_project_skeleton_branches.sh origin main
```

## 5. 创建 GitHub issues

```bash
export GITHUB_TOKEN=your_token_here
python3 scripts/github/create_project_skeleton_issues.py
```

默认目标仓库：

- `F91KO/ChengDD`

如需改仓库：

```bash
export GITHUB_REPOSITORY=owner/repo
python3 scripts/github/create_project_skeleton_issues.py
```

## 6. 创建结果

脚本会创建：

- 1 个骨架总览 epic issue
- 10 个细粒度骨架 issue

每个 issue 描述里会包含：

- 目标
- 范围
- 交付物
- 依赖关系
- 建议分支名

## 7. 建议执行顺序

1. 推送分支
2. 创建 issues
3. 在 GitHub 上为 issue 补 label 和 assignee
4. 开始按 issue 分支推进实现
