# GitHub 骨架任务执行说明

## 1. 说明

- 当前仓库内已准备好本地规划分支和 GitHub issue 创建脚本。
- 由于当前机器没有可用的 GitHub 认证，远端分支推送和 issue 创建未自动完成。
- 你完成 GitHub 认证后，可以直接按本文步骤执行。

## 2. 当前流程约束

- 后续开始任何开发任务前，先输出一版任务 list，明确本次范围、子任务拆分、依赖关系和验收点。
- 任务 list 确认后，再创建对应 GitHub issue，不允许跳过任务拆解直接开工。
- issue 创建后，再创建对应开发分支，原则上保持“一项任务一个 issue、一个主分支”。
- 代码实现、联调、验收、PR 提交都应围绕已确认的任务 list 和 issue 范围进行，避免边做边扩 scope。
- 若任务在执行中明显扩容，应先回写任务 list 和 issue，再继续实现。
- 涉及开发实现的任务，完成代码后必须执行自动化测试；若使用 sub-agent 执行测试，应由测试 sub-agent 输出测试报告后，主流程才可进入提交、PR 或合并阶段。
- 测试报告至少包含：测试范围、执行命令、结果摘要、失败项与原因、是否达到当前任务验收标准。

## 3. 当前授权约束

- 涉及 `git push`、`gh issue *`、`gh pr *`、分支创建、提交、验收脚本执行等操作时，优先复用已批准的持久化命令前缀授权。
- 若出现新的授权命令，优先申请稳定、可复用、范围适中的前缀授权，避免每次都为完整命令逐条确认。
- 不通过仓库文件维护授权，授权以前缀规则形式保存在当前 Codex / 执行环境侧。
- 破坏性命令不纳入持久化放行范围，例如 `rm`、`git reset --hard`、`git checkout --`。
- 后续执行 GitHub 和 Git 操作时，默认先检查是否已有可复用授权，再决定是否触发新的授权申请。
- 推荐配置清单见《[权限建议清单.md](./权限建议清单.md)》。

## 4. 已准备好的内容

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

## 5. 先完成 GitHub 认证

任选一种方式：

1. 配置 `gh auth login`
2. 配置 GitHub HTTPS 凭据
3. 导出环境变量 `GITHUB_TOKEN`

脚本要求：

- 远端推分支需要 Git push 权限
- 创建 issue 需要 `GITHUB_TOKEN`

## 6. 推送规划分支

```bash
bash scripts/github/push_project_skeleton_branches.sh
```

可选参数：

```bash
bash scripts/github/push_project_skeleton_branches.sh origin main
```

## 7. 创建 GitHub issues

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

## 8. 创建结果

脚本会创建：

- 1 个骨架总览 epic issue
- 10 个细粒度骨架 issue

每个 issue 描述里会包含：

- 目标
- 范围
- 交付物
- 依赖关系
- 建议分支名

## 9. 建议执行顺序

1. 先整理任务 list
2. 创建或补充对应 issue
3. 创建对应分支
4. 推送分支
5. 在 GitHub 上为 issue 补 label 和 assignee
6. 开始按 issue 分支推进实现
