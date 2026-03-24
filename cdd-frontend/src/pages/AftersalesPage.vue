<template>
  <WorkspaceLayout
    eyebrow="After-sales"
    title="售后处理"
    description="重点呈现售后单状态、商品信息、更新时间和快速处理动作，贴合运营日常节奏。"
  >
    <section :class="$style.tabs">
      <button
        v-for="tab in aftersalesTabs"
        :key="tab"
        :class="[$style.tab, tab === activeTab ? $style.tabActive : '']"
        @click="activeTab = tab"
      >
        {{ tab }}
      </button>
    </section>

    <UiStatePanel
      :tone="afterSaleStatePanel.tone"
      :title="afterSaleStatePanel.title"
      :description="afterSaleStatePanel.description"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.cards">
      <UiCard
        v-for="record in filteredRecords"
        :key="record.serviceNo"
        elevated
        :class="$style.card"
      >
        <div :class="$style.cardAccent"></div>
        <div :class="$style.cardBody">
          <div :class="$style.cardHead">
            <div>
              <div :class="$style.caption">售后单号 {{ record.serviceNo }}</div>
              <h3 :class="$style.orderNo">订单号 {{ record.orderNo }}</h3>
            </div>
            <UiTag :tone="record.statusTone as 'primary' | 'info' | 'danger' | 'success'">
              {{ record.status }}
            </UiTag>
          </div>

          <div :class="$style.summary">
            <div :class="$style.thumb">{{ record.type }}</div>
            <div>
              <div :class="$style.product">{{ record.product }}</div>
              <div :class="$style.spec">{{ record.spec }}</div>
              <div :class="$style.amount">{{ record.amount }}</div>
            </div>
          </div>

          <div :class="$style.footer">
            <div :class="$style.updated">更新于 {{ record.updateTime }}</div>
            <div :class="$style.actions">
              <UiButton variant="secondary" @click="handleViewDetail(record)">查看详情</UiButton>
              <UiButton v-if="record.tabStatus === '待处理'" @click="handleApprove(record)">快速同意</UiButton>
              <UiButton
                v-if="record.tabStatus === '待处理'"
                variant="secondary"
                @click="handleReject(record)"
              >
                快速驳回
              </UiButton>
            </div>
          </div>
        </div>
      </UiCard>
      <UiStatePanel
        v-if="filteredRecords.length === 0"
        tone="empty"
        title="当前状态下没有售后单"
        description="可切换到其他状态查看，或等待买家提交售后申请后再处理。"
      >
        <UiButton variant="secondary" @click="activeTab = '全部'">查看全部售后</UiButton>
      </UiStatePanel>
    </section>

    <UiCard v-if="detailRecord" elevated :class="$style.detailPanel">
      <div :class="$style.cardHead">
        <div>
          <div :class="$style.caption">售后详情</div>
          <h3 :class="$style.orderNo">{{ detailRecord.after_sale_no }}</h3>
        </div>
        <UiButton variant="secondary" @click="closeDetailPanel">关闭面板</UiButton>
      </div>

      <section :class="$style.detailGrid">
        <div>
          <div :class="$style.updated">商品</div>
          <div :class="$style.product">{{ detailRecord.product_name || '-' }}</div>
        </div>
        <div>
          <div :class="$style.updated">规格</div>
          <div :class="$style.product">{{ detailRecord.sku_name || '-' }}</div>
        </div>
        <div>
          <div :class="$style.updated">原因</div>
          <div :class="$style.product">{{ detailRecord.reason_desc || '-' }}</div>
        </div>
        <div>
          <div :class="$style.updated">退款金额</div>
          <div :class="$style.product">{{ formatCurrency(detailRecord.refund_amount) }}</div>
        </div>
      </section>

      <section :class="$style.detailGrid">
        <div>
          <div :class="$style.updated">退款单号</div>
          <div :class="$style.product">{{ detailRecord.refund_no || '-' }}</div>
        </div>
        <div>
          <div :class="$style.updated">退款状态</div>
          <div :class="$style.product">{{ detailRecord.refund_status || '-' }}</div>
        </div>
        <div>
          <div :class="$style.updated">退货物流</div>
          <div :class="$style.product">
            {{
              detailRecord.return_company && detailRecord.return_logistics_no
                ? `${detailRecord.return_company} / ${detailRecord.return_logistics_no}`
                : '-'
            }}
          </div>
        </div>
        <div>
          <div :class="$style.updated">商家处理结果</div>
          <div :class="$style.product">{{ detailRecord.merchant_result || '-' }}</div>
        </div>
      </section>

      <section :class="$style.detailBlock">
        <div :class="$style.sectionTitle">售后日志</div>
        <article
          v-for="log in detailLogs"
          :key="`${log.log_type}-${log.created_at}`"
          :class="$style.logRow"
        >
          <div>{{ log.log_type }}</div>
          <div>{{ log.message }}</div>
          <div>{{ formatDateTime(log.created_at) }}</div>
        </article>
      </section>

      <section v-if="detailTabStatus === '待处理'" :class="$style.detailBlock">
        <div :class="$style.sectionTitle">审核处理</div>
        <textarea
          v-model="approveRemark"
          :class="$style.textarea"
          placeholder="请输入审核意见"
        />
        <div :class="$style.actions">
          <UiButton variant="secondary" @click="approveRemark = ''">清空意见</UiButton>
          <UiButton variant="secondary" :disabled="submitting" @click="submitReview('reject')">
            {{ submitting ? '正在提交...' : '驳回申请' }}
          </UiButton>
          <UiButton :disabled="submitting" @click="submitReview('agree')">
            {{ submitting ? '正在提交...' : '同意申请' }}
          </UiButton>
        </div>
      </section>

      <section v-if="detailRecord.after_sale_status === 'waiting_return'" :class="$style.detailBlock">
        <div :class="$style.sectionTitle">退货物流录入</div>
        <div :class="$style.detailGrid">
          <label :class="$style.field">
            <span :class="$style.updated">物流公司</span>
            <input
              v-model="returnCompany"
              :class="$style.input"
              placeholder="请输入物流公司"
            />
          </label>
          <label :class="$style.field">
            <span :class="$style.updated">物流单号</span>
            <input
              v-model="returnLogisticsNo"
              :class="$style.input"
              placeholder="请输入物流单号"
            />
          </label>
        </div>
        <div :class="$style.actions">
          <UiButton variant="secondary" @click="resetReturnForm">清空物流信息</UiButton>
          <UiButton :disabled="submitting" @click="submitReturnLogistics">
            {{ submitting ? '正在提交...' : '提交退货物流' }}
          </UiButton>
        </div>
      </section>
    </UiCard>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { fetchAfterSaleDetail, fetchAfterSaleList, fetchAfterSaleLogs } from '@/services/aftersales';
import { reviewAfterSale, submitAfterSaleReturn } from '@/services/order';
import { useAuthStore } from '@/stores/auth';
import type {
  OrderAfterSaleDetailResponseRaw,
  OrderAfterSaleLogResponseRaw,
  OrderAfterSaleSummaryResponseRaw,
} from '@/types/aftersales';
import { aftersalesTabs } from '@/modules/aftersales/mock';

type AfterSaleCardRecord = {
  serviceNo: string;
  orderNo: string;
  product: string;
  spec: string;
  type: string;
  amount: string;
  status: string;
  tabStatus: string;
  statusTone: 'primary' | 'info' | 'danger' | 'success';
  updateTime: string;
  merchantId: number;
  storeId: number;
  userId: number;
};

const authStore = useAuthStore();
const activeTab = ref('全部');
const afterSalesMode = ref<'remote' | 'error'>('remote');
const afterSalesLoading = ref(true);
const afterSalesNotice = ref('正在加载真实售后接口。');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const records = ref<AfterSaleCardRecord[]>([]);
const detailRecord = ref<OrderAfterSaleDetailResponseRaw | null>(null);
const detailLogs = ref<OrderAfterSaleLogResponseRaw[]>([]);
const detailTabStatus = ref('');
const approveRemark = ref('同意售后申请，请按流程处理');
const returnCompany = ref('');
const returnLogisticsNo = ref('');
const submitting = ref(false);

const afterSaleStatePanel = computed(() => {
  if (afterSalesLoading.value) {
    return {
      tone: 'loading' as const,
      title: '正在加载售后列表',
      description: '页面正在加载真实售后接口数据。',
    };
  }

  if (afterSalesMode.value === 'remote') {
    return {
      tone: 'info' as const,
      title: '已接入真实售后列表接口',
      description: afterSalesNotice.value,
    };
  }

  return {
    tone: 'error' as const,
    title: '售后接口调用失败',
    description: afterSalesNotice.value,
  };
});

const filteredRecords = computed(() => {
  if (activeTab.value === '全部') {
    return records.value;
  }

  return records.value.filter((record) => record.tabStatus === activeTab.value);
});

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

function formatCurrency(value: number | string | null | undefined): string {
  const parsed = Number(value ?? 0);
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(parsed) ? parsed : 0);
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(date)
    .replace(/\//g, '-');
}

function resolveTypeLabel(type: string): string {
  if (type === 'refund_only') {
    return '仅退款';
  }
  if (type === 'return_refund') {
    return '退货退款';
  }
  return type || '售后';
}

function resolveStatus(status: string): {
  label: string;
  tabStatus: string;
  tone: AfterSaleCardRecord['statusTone'];
} {
  switch (status) {
    case 'pending_merchant':
      return { label: '待处理', tabStatus: '待处理', tone: 'primary' };
    case 'agreed':
      return { label: '已同意', tabStatus: '已同意', tone: 'info' };
    case 'rejected':
      return { label: '已驳回', tabStatus: '已驳回', tone: 'danger' };
    case 'waiting_return':
      return { label: '待退货', tabStatus: '待退货', tone: 'info' };
    case 'refunding':
      return { label: '退款中', tabStatus: '已同意', tone: 'info' };
    case 'completed':
      return { label: '已完成', tabStatus: '已完成', tone: 'success' };
    default:
      return { label: status || '处理中', tabStatus: '全部', tone: 'info' };
  }
}

function toCardRecord(item: OrderAfterSaleSummaryResponseRaw): AfterSaleCardRecord {
  const status = resolveStatus(item.after_sale_status);
  return {
    serviceNo: item.after_sale_no,
    orderNo: item.order_no,
    product: item.product_name || '待补充商品信息',
    spec: item.sku_name || '默认规格',
    type: resolveTypeLabel(item.after_sale_type),
    amount: formatCurrency(item.refund_amount),
    status: status.label,
    tabStatus: status.tabStatus,
    statusTone: status.tone,
    updateTime: formatDateTime(item.updated_at),
    merchantId: item.merchant_id,
    storeId: item.store_id,
    userId: item.user_id,
  };
}

function mapTabToStatus(tab: string): string | undefined {
  switch (tab) {
    case '待处理':
      return 'pending_merchant';
    case '已同意':
      return 'agreed';
    case '已驳回':
      return 'rejected';
    case '待退货':
      return 'waiting_return';
    case '已完成':
      return 'completed';
    default:
      return undefined;
  }
}

async function handleViewDetail(record: AfterSaleCardRecord) {
  try {
    detailRecord.value = await fetchAfterSaleDetail({
      afterSaleNo: record.serviceNo,
      merchantId: record.merchantId,
      storeId: record.storeId,
    });
    detailLogs.value = await fetchAfterSaleLogs({
      afterSaleNo: record.serviceNo,
      merchantId: record.merchantId,
      storeId: record.storeId,
    });
    detailTabStatus.value = record.tabStatus;
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '查询售后详情失败。', 'error');
  }
}

async function handleApprove(record: AfterSaleCardRecord) {
  await handleViewDetail(record);
}

async function handleReject(record: AfterSaleCardRecord) {
  await handleViewDetail(record);
  approveRemark.value = '驳回售后申请，请补充凭证后再提交';
}

async function submitReview(reviewAction: 'agree' | 'reject') {
  try {
    const operatorId = authStore.userIdForQuery;
    if (!operatorId) {
      throw new Error('当前上下文缺少操作人 ID。');
    }
    if (!detailRecord.value) {
      throw new Error('请先选择售后单。');
    }
    submitting.value = true;
    await reviewAfterSale({
      afterSaleNo: detailRecord.value.after_sale_no,
      merchantId: detailRecord.value.merchant_id,
      storeId: detailRecord.value.store_id,
      operatorId,
      reviewAction,
      merchantResult: approveRemark.value.trim(),
    });
    await loadAfterSales();
    setActionMessage(
      `售后单 ${detailRecord.value.after_sale_no} 已${reviewAction === 'agree' ? '同意' : '驳回'}。`,
    );
    closeDetailPanel();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '审核售后失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

async function submitReturnLogistics() {
  try {
    if (!detailRecord.value) {
      throw new Error('请先选择售后单。');
    }
    if (!returnCompany.value.trim() || !returnLogisticsNo.value.trim()) {
      throw new Error('请完整填写退货物流公司和单号。');
    }
    submitting.value = true;
    await submitAfterSaleReturn({
      afterSaleNo: detailRecord.value.after_sale_no,
      merchantId: detailRecord.value.merchant_id,
      storeId: detailRecord.value.store_id,
      userId: detailRecord.value.user_id,
      returnCompany: returnCompany.value.trim(),
      returnLogisticsNo: returnLogisticsNo.value.trim(),
    });
    await loadAfterSales();
    setActionMessage(`售后单 ${detailRecord.value.after_sale_no} 已提交退货物流。`);
    closeDetailPanel();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '提交退货物流失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

async function loadAfterSales() {
  afterSalesLoading.value = true;
  await authStore.ensureCurrentContext();

  const merchantId = authStore.merchantIdForQuery ?? 1001;
  const storeId = authStore.storeIdForQuery ?? 1001;
  try {
    const response = await fetchAfterSaleList({
      merchantId,
      storeId,
      afterSaleStatus: mapTabToStatus(activeTab.value),
    });
    afterSalesMode.value = 'remote';
    afterSalesNotice.value = response.length
      ? '售后列表已连接真实接口。'
      : '当前筛选条件下没有真实售后单。';
    records.value = response.map(toCardRecord);
  } catch (error) {
    afterSalesMode.value = 'error';
    afterSalesNotice.value = error instanceof Error ? error.message : '售后服务未就绪。';
    records.value = [];
  } finally {
    afterSalesLoading.value = false;
  }
}

onMounted(() => {
  void loadAfterSales();
});

watch(activeTab, () => {
  void loadAfterSales();
});

function closeDetailPanel() {
  detailRecord.value = null;
  detailLogs.value = [];
  detailTabStatus.value = '';
  approveRemark.value = '同意售后申请，请按流程处理';
  resetReturnForm();
}

function resetReturnForm() {
  returnCompany.value = '';
  returnLogisticsNo.value = '';
}
</script>

<style module>
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tab {
  min-height: 44px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: var(--cdd-text-soft);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
  font-size: 13px;
  font-weight: 800;
}

.tabActive {
  color: #fff;
  background: linear-gradient(135deg, var(--cdd-primary-deep), var(--cdd-primary));
}

.cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.detailPanel,
.detailBlock {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.logRow {
  display: grid;
  grid-template-columns: 0.8fr 1.6fr 1fr;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
}

.input,
.textarea {
  min-height: 96px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  resize: vertical;
}

.input {
  min-height: 54px;
  resize: none;
}

.card {
  position: relative;
  overflow: hidden;
}

.cardAccent {
  position: absolute;
  left: 0;
  top: 0;
  width: 6px;
  height: 100%;
  background: linear-gradient(180deg, var(--cdd-primary), var(--cdd-primary-deep));
}

.cardBody {
  display: grid;
  gap: 20px;
  padding: 24px 24px 24px 28px;
}

.cardHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.caption {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.orderNo {
  margin: 8px 0 0;
  font-size: 18px;
}

.summary {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.thumb {
  min-height: 112px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(255, 107, 0, 0.16), transparent 35%),
    linear-gradient(145deg, #eff5ff, #d9eaff);
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 800;
}

.product {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.5;
}

.spec {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  font-size: 13px;
}

.amount {
  margin-top: 12px;
  color: var(--cdd-primary);
  font-size: 24px;
  font-weight: 800;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.updated {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 1100px) {
  .cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .cardHead,
  .summary,
  .footer,
  .actions,
  .detailGrid,
  .logRow {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .actions {
    width: 100%;
  }
}
</style>
