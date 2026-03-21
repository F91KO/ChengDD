# 程哒哒状态流转矩阵与 SQL 落库说明

## 1. 文档说明

- 本文用于把状态机图、数据库设计实施细化和正式迁移脚本衔接起来。
- 目标是回答三个问题：
  - 一个动作会把哪些状态从什么值改成什么值。
  - 这个动作要更新哪些表、哪些字段。
  - 这些更新应该放在一个本地事务里，还是通过异步/补偿完成。
- 本文优先覆盖一期高风险链路：商家入驻、小程序接入、发布任务、订单支付、售后退款。

## 2. 落库总原则

### 2.1 单一事实表原则

- 一个业务对象只允许一个主状态事实表。
- 过程日志、审计日志、步骤日志用于补充追踪，不反向驱动主状态。

对应关系：

| 业务对象 | 主状态表 | 主状态字段 |
| --- | --- | --- |
| 商家入驻申请 | `cdd_merchant_application` | `status` |
| 商家主体 | `cdd_merchant_profile` | `status` |
| 店铺 | `cdd_merchant_store` | `business_status` |
| 小程序接入 | `cdd_merchant_mini_program` | `binding_status` |
| 商品 | `cdd_product_spu` | `status`、`publish_check_status` |
| 订单 | `cdd_order_info` | `order_status`、`pay_status`、`delivery_status` |
| 支付流水 | `cdd_order_pay_record` | `pay_status` |
| 退款流水 | `cdd_order_refund_record` | `refund_status` |
| 售后单 | `cdd_order_after_sale` | `after_sale_status` |
| 发布任务 | `cdd_release_task` | `release_status` |
| 发布步骤 | `cdd_release_task_detail` | `step_status` |

### 2.2 先主表，后日志

- 同一事务内先更新主状态表，再插入状态日志/审计日志。
- 日志写入失败时，整体事务回滚，不允许主表成功但日志缺失。
- 报表类更新不在主事务内处理。

### 2.3 幂等优先级

| 场景 | 幂等键 |
| --- | --- |
| 支付回调 | `pay_no`、`third_party_trade_no` |
| 退款回调 | `refund_no`、`third_party_refund_no` |
| 发布步骤回调 | `task_id + step_code` |
| 商家申请审核 | `application_id + 操作类型 + version` |
| 开通初始化 | `merchant_id + store_id + mini_program_id` |

## 3. 商家入驻与开通链路

### 3.1 申请提交

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 商家提交申请 | `draft`、`supplement_required`、`rejected` | `submitted` | `cdd_merchant_application.status`、`submitted_at` | 单表事务 |

落库规则：
- `submitted_at` 仅首次提交为空时写入，后续重提允许覆盖为最新提交时间。
- 提交前应用层必须校验 `legal_person_name`、`business_category`、`license_file_url` 非空。

SQL 落库建议：

```sql
UPDATE cdd_merchant_application
SET
  status = 'submitted',
  submitted_at = NOW(),
  updated_by = ?,
  version = version + 1
WHERE id = ?
  AND deleted = 0
  AND status IN ('draft', 'supplement_required', 'rejected')
  AND version = ?;
```

### 3.2 平台开始审核

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 平台受理申请 | `submitted` | `reviewing` | `cdd_merchant_application.status` | 单表事务 |

### 3.3 要求补资料

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 平台要求补资料 | `reviewing` | `supplement_required` | `cdd_merchant_application.status` | 单表事务 |

补充规则：
- 平台原因建议同时回写到 `reject_reason`，但语义上不是最终驳回。
- 若后续要区分“补充原因”和“驳回原因”，建议新增独立字段，不复用。

### 3.4 审核拒绝

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 平台驳回申请 | `reviewing` | `rejected` | `cdd_merchant_application.status`、`reject_reason` | 单表事务 |

### 3.5 审核通过并开主体

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 平台审核通过 | `reviewing` | `approved` | `cdd_merchant_application`、`cdd_merchant_profile`、`cdd_merchant_store` | 本地事务 |

本地事务内建议顺序：

1. 更新 `cdd_merchant_application.status='approved'`
2. 插入 `cdd_merchant_profile`
3. 插入默认 `cdd_merchant_store`
4. 写入 `cdd_audit_log`

SQL 落库说明：
- `cdd_merchant_profile.status` 初始值建议为 `pending_activation`
- `cdd_merchant_store.business_status` 初始值建议为 `draft`

### 3.6 小程序接入检测

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 发起检测 | `unbound`、`bind_failed` | `detecting` | `cdd_merchant_mini_program.binding_status` | 单表事务 |
| 检测通过 | `detecting` | `bound` | `cdd_merchant_mini_program.binding_status`、`last_detect_result_json` | 单表事务 |
| 检测失败 | `detecting` | `bind_failed` | `cdd_merchant_mini_program.binding_status`、`last_detect_result_json` | 单表事务 |

### 3.7 一键开通完成

| 动作 | 前置条件 | 更新表 | 事务 |
| --- | --- | --- | --- |
| 商家一键开通成功 | 小程序已 `bound`，模板已选定 | `cdd_merchant_profile.status='active'`、`cdd_merchant_store.business_status='open'`、初始化装修/分类/发布任务 | 主表初始化用本地事务，跨服务编排异步推进 |

落库要点：
- 商家主体激活和默认店铺开店可在本地事务内完成。
- 分类初始化、装修初始化、发布任务创建可以由编排服务串联，但不应跨库强事务。

## 4. 商品发布链路

### 4.1 商品保存草稿

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 保存商品 | 任意可编辑态 | `status='draft'` | `cdd_product_spu`、`cdd_product_sku`、`cdd_product_stock` | 本地事务 |

### 4.2 发起发布校验

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 发起一键发布 | `draft`、`offline` | `publish_check_status='pending'` | `cdd_product_spu.publish_check_status` | 单表事务 |

### 4.3 校验通过上架

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 校验通过 | `publish_check_status='pending'` | `status='online'`、`publish_check_status='passed'` | `cdd_product_spu`、`cdd_product_publish_record` | 本地事务 |

### 4.4 校验失败

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 校验失败 | `publish_check_status='pending'` | `publish_check_status='failed'` | `cdd_product_spu`、`cdd_product_publish_record` | 本地事务 |

### 4.5 手动下架

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 手动下架 | `status='online'` | `status='offline'` | `cdd_product_spu.status` | 单表事务 |

## 5. 发布任务链路

### 5.1 创建发布任务

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 创建任务 | 无 | `release_status='created'` | `cdd_release_task`、`cdd_release_task_detail` | 本地事务 |

落库规则：
- 主任务创建后立即生成固定步骤明细，例如 `precheck`、`package`、`submit`、`result_sync`
- 每个 `step_code` 在一个任务内唯一，由 `uk_task_step_code_deleted` 保证

### 5.2 校验通过进入待发布

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 前置校验通过 | `created`、`checking` | `ready` | `cdd_release_task.release_status`、`cdd_release_task_detail.step_status` | 本地事务 |

### 5.3 校验失败

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 前置校验失败 | `created`、`checking` | `failed` | `cdd_release_task.release_status`、失败步骤 `step_status='failed'`、`result_message` | 本地事务 |

### 5.4 开始发布

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 执行发布 | `ready` | `running` | `cdd_release_task.release_status`、当前步骤 `step_status='running'`、`started_at` | 本地事务 |

### 5.5 步骤成功

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 某步骤执行成功 | `step_status='running'` | `step_status='success'` | `cdd_release_task_detail`、`cdd_release_log` | 本地事务 |

### 5.6 全部步骤完成

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 全部步骤完成 | `running` | `success` | `cdd_release_task.release_status`、`finished_at`，并更新 `cdd_merchant_mini_program.current_template_version` | 主任务事务 + 外围补偿 |

说明：
- `current_template_version` 更新属于发布结果生效动作。
- 如果任务成功但模板版本回写失败，需要补偿任务重新对齐，不应把主任务状态回滚为 `running`。

### 5.7 失败重试

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 修复后重试 | `failed` | `checking` | `cdd_release_task.release_status`，失败步骤重置为 `pending` | 本地事务 |

### 5.8 回滚

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 执行回滚 | `success`、`failed` | 主任务可保留原状态或新建回滚任务 | `cdd_release_rollback_record`、`cdd_release_log` | 建议新任务编排，不直接覆写原任务 |

落库建议：
- 回滚最好新建一条 `release_type='rollback'` 的 `cdd_release_task`
- `cdd_release_rollback_record` 只作为附属记录，不承担主编排

## 6. 订单与支付链路

### 6.1 提交订单

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 用户提交订单 | 无 | `order_status='pending_pay'`、`pay_status='unpaid'` | `cdd_order_checkout_snapshot`、`cdd_order_info`、`cdd_order_item`、`cdd_order_status_log` | 本地事务 |

本地事务顺序：

1. 校验快照和库存
2. 插入 `cdd_order_info`
3. 插入 `cdd_order_item`
4. 插入 `cdd_order_status_log`

### 6.2 发起支付

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 创建支付单 | `pay_status='unpaid'` | `pay_status='paying'` | `cdd_order_pay_record`、`cdd_order_info.pay_status` | 本地事务 |

### 6.3 支付成功回调

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 第三方支付成功 | `order_status='pending_pay'`、`pay_status='paying'/'unpaid'` | `order_status='paid'`、`pay_status='paid'`、`delivery_status='pending'` | `cdd_order_pay_record`、`cdd_order_info`、`cdd_order_status_log` | 本地事务 |

SQL 落库建议：

```sql
UPDATE cdd_order_pay_record
SET
  pay_status = 'success',
  third_party_trade_no = ?,
  pay_response_json = ?,
  paid_at = NOW(),
  updated_by = ?,
  version = version + 1
WHERE pay_no = ?
  AND deleted = 0
  AND pay_status IN ('created', 'paying')
  AND version = ?;
```

```sql
UPDATE cdd_order_info
SET
  order_status = 'paid',
  pay_status = 'paid',
  delivery_status = 'pending',
  paid_amount = ?,
  paid_at = NOW(),
  updated_by = ?,
  version = version + 1
WHERE id = ?
  AND deleted = 0
  AND order_status = 'pending_pay'
  AND pay_status IN ('unpaid', 'paying')
  AND version = ?;
```

### 6.4 超时取消

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 超时关单 | `order_status='pending_pay'` | `order_status='cancelled'` | `cdd_order_info`、`cdd_order_status_log`，必要时关闭支付单 | 本地事务 |

### 6.5 商家履约

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 开始备货 | `delivery_status='pending'` | `delivery_status='preparing'` | `cdd_order_info` | 单表事务 |
| 发货/履约完成 | `delivery_status='preparing'` | `delivery_status='shipped'` | `cdd_order_info`、`cdd_order_status_log` | 本地事务 |
| 用户确认收货 | `delivery_status='shipped'` | `delivery_status='received'`、`order_status='finished'` | `cdd_order_info`、`cdd_order_status_log` | 本地事务 |

## 7. 售后与退款链路

### 7.1 用户发起售后

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 发起售后 | 无 | `after_sale_status='applied'` | `cdd_order_after_sale`，必要时更新 `cdd_order_item.refund_status` | 本地事务 |

### 7.2 商家审核售后

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 商家受理 | `applied` | `reviewing` | `cdd_order_after_sale` | 单表事务 |
| 商家同意 | `reviewing` | `approved` | `cdd_order_after_sale` | 单表事务 |
| 商家拒绝 | `reviewing` | `rejected` 或 `closed` | `cdd_order_after_sale` | 单表事务 |

### 7.3 创建退款单

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 进入退款 | `approved` | `refunding` | `cdd_order_after_sale`、`cdd_order_refund_record` | 本地事务 |

### 7.4 退款成功

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 第三方退款成功 | `refund_status='processing'/'created'` | `refund_status='success'`、售后 `finished` | `cdd_order_refund_record`、`cdd_order_after_sale`、`cdd_order_info.pay_status` | 本地事务 |

支付侧联动规则：
- 全额退款：`cdd_order_info.pay_status='refund_full'`
- 部分退款：`cdd_order_info.pay_status='refund_partial'`

### 7.5 退款失败

| 动作 | 当前状态 | 下一状态 | 更新表 | 事务 |
| --- | --- | --- | --- | --- |
| 第三方退款失败 | `refund_status='processing'` | `refund_status='failed'`、售后维持 `refunding` 或转人工 | `cdd_order_refund_record`、`cdd_order_after_sale` | 本地事务 |

## 8. 日志与审计写入规则

### 8.1 订单状态日志

写入时机：
- 订单创建
- 支付成功
- 超时取消
- 履约发货
- 确认收货

落库表：
- `cdd_order_status_log`

建议字段映射：

| 字段 | 来源 |
| --- | --- |
| `order_id` | 订单主键 |
| `order_no` | 订单号 |
| `from_status` | 原 `order_status` |
| `to_status` | 新 `order_status` |
| `operate_type` | `create`、`pay_success`、`timeout_cancel`、`ship`、`finish` |
| `operator_id` | 用户、商家账号或系统任务 |

### 8.2 发布步骤日志

写入时机：
- 每个发布步骤开始
- 每个发布步骤结束
- 整体任务失败

落库表：
- `cdd_release_task_detail`
- `cdd_release_log`

### 8.3 审计日志

需要写 `cdd_audit_log` 的动作：
- 平台审核商家申请
- 平台发布模板版本
- 商家修改店铺核心资料
- 商家执行一键开通
- 平台或商家发起回滚

## 9. 与现有状态机图的对齐说明

- [状态机图.md](../01-product/状态机图.md) 当前更偏业务语义，部分状态名和数据库字段值不完全一致。
- 数据库和代码落地时，优先以本文及 [数据库设计实施细化.md](./数据库设计实施细化.md) 的状态字典为准。
- 若后续需要统一图和库，建议把状态机图中的以下名称收敛：
  - `pending_process` -> `reviewing`
  - `pending_check` / `pending_release` / `releasing` -> `checking` / `ready` / `running`
  - `partial_success` 作为步骤或结果语义，不一定进入主任务状态
  - `completed` -> `finished`

## 10. 下一步建议

建议继续补两项：

1. 幂等/补偿表设计，见 [幂等与补偿表设计](./幂等与补偿表设计.md)。
2. 把本文中的关键状态流转 SQL 进一步拆成应用层 Repository 方法约定。
