<template>
  <WorkspaceLayout
    eyebrow="After-sales"
    title="售后处理"
    description="按状态查看售后单，联动详情、审核和退货物流代录，让日常处理动作尽量在一个页面内完成。"
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
      <UiCard v-for="record in filteredRecords" :key="record.serviceNo" elevated :class="$style.card">
        <div :class="$style.cardAccent"></div>
        <div :class="$style.cardBody">
          <div :class="$style.cardHead">
            <div>
              <div :class="$style.caption">售后单号 {{ record.serviceNo }}</div>
              <h3 :class="$style.orderNo">订单 {{ record.orderNo }}</h3>
            </div>
            <UiTag :tone="record.statusTone as 'primary' | 'info' | 'danger' | 'success'">
              {{ record.status }}
            </UiTag>
          </div>

          <div :class="$style.summary">
            <div :class="$style.thumb">
              <div :class="$style.thumbType">{{ record.type }}</div>
              <div :class="$style.thumbCount">{{ record.quantityText }}</div>
            </div>
            <div>
              <div :class="$style.product">{{ record.product }}</div>
              <div :class="$style.spec">{{ record.spec }}</div>
              <div :class="$style.amount">{{ record.amount }}</div>
            </div>
          </div>

          <div :class="$style.metaGrid">
            <div>
              <div :class="$style.metaLabel">原因</div>
              <div :class="$style.metaValue">{{ record.reason }}</div>
            </div>
            <div>
              <div :class="$style.metaLabel">最近更新时间</div>
              <div :class="$style.metaValue">{{ record.updateTime }}</div>
            </div>
          </div>

          <div :class="$style.footer">
            <div :class="$style.updated">商家 {{ record.merchantId }} / 门店 {{ record.storeId }}</div>
            <div :class="$style.actions">
              <UiButton variant="secondary" @click="handleViewDetail(record)">查看详情</UiButton>
              <UiButton
                v-if="record.tabStatus === '待处理'"
                :disabled="submitting"
                @click="submitQuickReview(record, 'agree')"
              >
                {{ submitting ? '正在提交...' : '快速同意' }}
              </UiButton>
              <UiButton
                v-if="record.tabStatus === '待处理'"
                variant="secondary"
                :disabled="submitting"
                @click="submitQuickReview(record, 'reject')"
              >
                {{ submitting ? '正在提交...' : '快速驳回' }}
              </UiButton>
            </div>
          </div>
        </div>
      </UiCard>

      <UiStatePanel
        v-if="filteredRecords.length === 0"
        tone="empty"
        title="当前状态下没有售后单"
        description="可以切换到其他状态查看，或者等待新的用户售后申请。"
        >
          <UiButton variant="secondary" @click="activeTab = '全部'">查看全部售后</UiButton>
        </UiStatePanel>
    </section>

    <UiPagination
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      :disabled="afterSalesLoading"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <UiCard v-if="detailRecord" ref="detailAnchor" elevated :class="$style.detailPanel">
      <div :class="$style.cardHead">
        <div>
          <div :class="$style.caption">售后详情</div>
          <h3 :class="$style.orderNo">{{ detailRecord.after_sale_no }}</h3>
        </div>
        <UiButton variant="secondary" @click="closeDetailPanel">关闭面板</UiButton>
      </div>

      <section :class="$style.detailGrid">
        <div>
          <div :class="$style.metaLabel">商品</div>
          <div :class="$style.product">{{ detailRecord.product_name || '-' }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">规格</div>
          <div :class="$style.product">{{ detailRecord.sku_name || '-' }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">申请类型</div>
          <div :class="$style.product">{{ resolveTypeLabel(detailRecord.after_sale_type) }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">退款金额</div>
          <div :class="$style.product">{{ formatCurrency(detailRecord.refund_amount) }}</div>
        </div>
      </section>

      <section :class="$style.detailGrid">
        <div>
          <div :class="$style.metaLabel">售后状态</div>
          <div :class="$style.product">{{ resolveStatus(detailRecord.after_sale_status).label }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">原因说明</div>
          <div :class="$style.product">{{ detailRecord.reason_desc || '-' }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">退款单号</div>
          <div :class="$style.product">{{ detailRecord.refund_no || '-' }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">退款状态</div>
          <div :class="$style.product">{{ formatRefundStatus(detailRecord.refund_status) }}</div>
        </div>
      </section>

      <section :class="$style.detailGrid">
        <div>
          <div :class="$style.metaLabel">商家处理结果</div>
          <div :class="$style.product">{{ detailRecord.merchant_result || '-' }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">退货物流</div>
          <div :class="$style.product">
            {{
              detailRecord.return_company && detailRecord.return_logistics_no
                ? `${detailRecord.return_company} / ${detailRecord.return_logistics_no}`
                : '-'
            }}
          </div>
        </div>
        <div>
          <div :class="$style.metaLabel">申请时间</div>
          <div :class="$style.product">{{ formatDateTime(detailRecord.created_at) }}</div>
        </div>
        <div>
          <div :class="$style.metaLabel">更新时间</div>
          <div :class="$style.product">{{ formatDateTime(detailRecord.updated_at) }}</div>
        </div>
      </section>

      <section v-if="detailRecord.proof_urls.length" :class="$style.detailBlock">
        <div :class="$style.sectionTitle">凭证图片</div>
        <div :class="$style.proofGrid">
          <a
            v-for="(proofUrl, index) in detailRecord.proof_urls"
            :key="`${proofUrl}-${index}`"
            :href="proofUrl"
            target="_blank"
            rel="noreferrer"
            :class="$style.proofItem"
          >
            凭证 {{ index + 1 }}
          </a>
        </div>
      </section>

      <section :class="$style.detailBlock">
        <div :class="$style.sectionTitle">售后日志</div>
        <article
          v-for="log in detailLogs"
          :key="`${log.log_type}-${log.created_at}`"
          :class="$style.logRow"
        >
          <div>
            <div :class="$style.metaLabel">类型</div>
            <div :class="$style.metaValue">{{ formatLogType(log.log_type) }}</div>
          </div>
          <div>
            <div :class="$style.metaLabel">说明</div>
            <div :class="$style.metaValue">{{ log.message }}</div>
          </div>
          <div>
            <div :class="$style.metaLabel">时间</div>
            <div :class="$style.metaValue">{{ formatDateTime(log.created_at) }}</div>
          </div>
        </article>
      </section>

      <section v-if="detailRecord.after_sale_status === 'pending_merchant'" :class="$style.detailBlock">
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
        <div :class="$style.sectionTitle">退货物流代录</div>
        <div :class="$style.detailGrid">
          <label :class="$style.field">
            <span :class="$style.metaLabel">物流公司</span>
            <input
              v-model="returnCompany"
              :class="$style.input"
              placeholder="请输入用户退回的物流公司"
            />
          </label>
          <label :class="$style.field">
            <span :class="$style.metaLabel">物流单号</span>
            <input
              v-model="returnLogisticsNo"
              :class="$style.input"
              placeholder="请输入用户退回的物流单号"
            />
          </label>
        </div>
        <div :class="$style.actions">
          <UiButton variant="secondary" @click="resetReturnForm">清空物流信息</UiButton>
          <UiButton :disabled="submitting" @click="submitReturnLogistics">
            {{ submitting ? '正在提交...' : '提交代录物流' }}
          </UiButton>
        </div>
      </section>
    </UiCard>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiPagination from '@/components/base/UiPagination.vue';
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

type AfterSaleCardRecord = {
  serviceNo: string;
  orderNo: string;
  product: string;
  spec: string;
  type: string;
  amount: string;
  quantityText: string;
  reason: string;
  status: string;
  tabStatus: string;
  statusTone: 'primary' | 'info' | 'danger' | 'success';
  updateTime: string;
  merchantId: number;
  storeId: number;
  userId: number;
};

const aftersalesTabs = ['全部', '待处理', '已同意', '已驳回', '待退货', '已完成'];

const authStore = useAuthStore();
const activeTab = ref('全部');
const afterSalesMode = ref<'remote' | 'error'>('remote');
const afterSalesLoading = ref(true);
const afterSalesNotice = ref('正在加载真实售后接口。');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const records = ref<AfterSaleCardRecord[]>([]);
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
});
const detailRecord = ref<OrderAfterSaleDetailResponseRaw | null>(null);
const detailLogs = ref<OrderAfterSaleLogResponseRaw[]>([]);
const approveRemark = ref('同意售后申请，请按流程继续处理。');
const returnCompany = ref('');
const returnLogisticsNo = ref('');
const submitting = ref(false);
const detailAnchor = ref<HTMLElement | null>(null);

const afterSaleStatePanel = computed(() => {
  if (afterSalesLoading.value) {
    return {
      tone: 'loading' as const,
      title: '正在加载售后列表',
      description: '页面正在读取真实售后数据。',
    };
  }

  if (afterSalesMode.value === 'remote') {
    return {
      tone: 'info' as const,
      title: '已接入真实售后接口',
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
  return records.value;
});

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

function scrollDetailIntoView() {
  void nextTick(() => {
    detailAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
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

function formatRefundStatus(status: string | null | undefined): string {
  const normalized = (status || '').toLowerCase();
  if (!normalized) {
    return '-';
  }
  if (normalized.includes('pending')) {
    return '待退款';
  }
  if (normalized.includes('processing') || normalized.includes('ing')) {
    return '退款中';
  }
  if (normalized.includes('success') || normalized.includes('completed') || normalized.includes('done')) {
    return '退款成功';
  }
  if (normalized.includes('fail') || normalized.includes('close') || normalized.includes('reject')) {
    return '退款失败';
  }
  return status || '-';
}

function formatLogType(type: string | null | undefined): string {
  const normalized = (type || '').toLowerCase();
  if (!normalized) {
    return '-';
  }
  if (normalized.includes('apply')) {
    return '用户发起申请';
  }
  if (normalized.includes('merchant_review')) {
    return '商家审核';
  }
  if (normalized.includes('merchant_agree')) {
    return '商家同意';
  }
  if (normalized.includes('merchant_reject')) {
    return '商家驳回';
  }
  if (normalized.includes('refund_success')) {
    return '退款成功';
  }
  if (normalized.includes('refund_failed')) {
    return '退款失败';
  }
  if (normalized.includes('return')) {
    return '代录退货物流';
  }
  if (normalized.includes('refund')) {
    return '退款处理';
  }
  if (normalized.includes('complete')) {
    return '售后完成';
  }
  if (normalized.includes('close')) {
    return '售后关闭';
  }
  if (normalized.includes('system')) {
    return '系统处理';
  }
  return type || '-';
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
    quantityText: `件数 ${item.refund_quantity ?? 1}`,
    reason: item.reason_desc || '未填写原因',
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
      return 'agreed,refunding';
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
    scrollDetailIntoView();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '查询售后详情失败。', 'error');
  }
}

async function submitQuickReview(record: AfterSaleCardRecord, reviewAction: 'agree' | 'reject') {
  try {
    const operatorId = authStore.userIdForQuery;
    if (!operatorId) {
      throw new Error('当前上下文缺少操作人 ID。');
    }

    submitting.value = true;
    const merchantResult = reviewAction === 'agree'
      ? '同意售后申请，请按流程继续处理。'
      : '驳回售后申请，请补充凭证后重新提交。';
    const lifecycle = await reviewAfterSale({
      afterSaleNo: record.serviceNo,
      merchantId: record.merchantId,
      storeId: record.storeId,
      operatorId,
      reviewAction,
      merchantResult,
    });
    await loadAfterSales();
    if (detailRecord.value?.after_sale_no === record.serviceNo) {
      closeDetailPanel();
    }
    setActionMessage(
      `售后单 ${record.serviceNo} 已${reviewAction === 'agree' ? '同意' : '驳回'}，当前状态为 ${resolveStatus(lifecycle.after_sale_status).label}。`,
    );
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '快速审核售后失败。', 'error');
  } finally {
    submitting.value = false;
  }
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
    const lifecycle = await reviewAfterSale({
      afterSaleNo: detailRecord.value.after_sale_no,
      merchantId: detailRecord.value.merchant_id,
      storeId: detailRecord.value.store_id,
      operatorId,
      reviewAction,
      merchantResult: approveRemark.value.trim(),
    });
    await loadAfterSales();
    setActionMessage(
      `售后单 ${detailRecord.value.after_sale_no} 已${reviewAction === 'agree' ? '同意' : '驳回'}，当前状态为 ${resolveStatus(lifecycle.after_sale_status).label}。`,
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
    const operatorId = authStore.userIdForQuery;
    if (!operatorId) {
      throw new Error('当前上下文缺少操作人 ID。');
    }
    if (!returnCompany.value.trim() || !returnLogisticsNo.value.trim()) {
      throw new Error('请完整填写用户退货物流公司和单号。');
    }

    submitting.value = true;
    await submitAfterSaleReturn({
      afterSaleNo: detailRecord.value.after_sale_no,
      merchantId: detailRecord.value.merchant_id,
      storeId: detailRecord.value.store_id,
      userId: detailRecord.value.user_id,
      operatorId,
      returnCompany: returnCompany.value.trim(),
      returnLogisticsNo: returnLogisticsNo.value.trim(),
    });
    await loadAfterSales();
    setActionMessage(`售后单 ${detailRecord.value.after_sale_no} 已代录退货物流。`);
    closeDetailPanel();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '代录退货物流失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

async function loadAfterSales() {
  afterSalesLoading.value = true;
  await authStore.ensureCurrentContext();

  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;

  try {
    if (!merchantId || !storeId) {
      throw new Error('当前账号缺少商家或店铺上下文，无法加载售后数据。');
    }
    const initialResponse = await fetchAfterSaleList({
      merchantId,
      storeId,
      afterSaleStatus: mapTabToStatus(activeTab.value),
      page: pagination.page,
      pageSize: pagination.pageSize,
    });
    const fallbackPage = initialResponse.total > 0 && initialResponse.list.length === 0 && initialResponse.page > 1
      ? Math.max(1, Math.ceil(initialResponse.total / initialResponse.page_size))
      : null;
    const response = fallbackPage
      ? await fetchAfterSaleList({
        merchantId,
        storeId,
        afterSaleStatus: mapTabToStatus(activeTab.value),
        page: fallbackPage,
        pageSize: initialResponse.page_size,
      })
      : initialResponse;
    afterSalesMode.value = 'remote';
    pagination.page = response.page;
    pagination.pageSize = response.page_size;
    pagination.total = response.total;
    afterSalesNotice.value = response.total
      ? `当前列表展示的是售后服务真实数据，共 ${response.total} 条。`
      : '当前筛选条件下没有售后单。';
    records.value = response.list.map(toCardRecord);
  } catch (error) {
    afterSalesMode.value = 'error';
    afterSalesNotice.value = error instanceof Error ? error.message : '售后服务暂未就绪。';
    pagination.total = 0;
    records.value = [];
  } finally {
    afterSalesLoading.value = false;
  }
}

function handlePageChange(page: number) {
  pagination.page = page;
  void loadAfterSales();
}

function handlePageSizeChange(pageSize: number) {
  pagination.page = 1;
  pagination.pageSize = pageSize;
  void loadAfterSales();
}

function closeDetailPanel() {
  detailRecord.value = null;
  detailLogs.value = [];
  approveRemark.value = '同意售后申请，请按流程继续处理。';
  resetReturnForm();
}

function resetReturnForm() {
  returnCompany.value = '';
  returnLogisticsNo.value = '';
}

onMounted(() => {
  void loadAfterSales();
});

watch(activeTab, () => {
  pagination.page = 1;
  void loadAfterSales();
});
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

.cardBody,
.detailPanel,
.detailBlock {
  display: grid;
  gap: 18px;
}

.cardBody {
  padding: 24px 24px 24px 28px;
}

.detailPanel {
  margin-top: 18px;
  padding: 24px;
}

.cardHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
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
  min-height: 120px;
  display: grid;
  gap: 8px;
  place-content: center;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(255, 107, 0, 0.16), transparent 35%),
    linear-gradient(145deg, #eff5ff, #d9eaff);
  color: var(--cdd-text-soft);
  text-align: center;
}

.thumbType {
  font-size: 14px;
  font-weight: 800;
}

.thumbCount {
  font-size: 12px;
  font-weight: 700;
}

.product {
  font-size: 16px;
  font-weight: 800;
  line-height: 1.6;
  word-break: break-word;
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

.metaGrid,
.detailGrid,
.proofGrid {
  display: grid;
  gap: 16px;
}

.metaGrid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detailGrid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.proofGrid {
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.metaLabel,
.updated {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.metaValue {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  line-height: 1.7;
}

.proofItem {
  display: grid;
  place-items: center;
  min-height: 92px;
  border-radius: 18px;
  text-decoration: none;
  color: var(--cdd-primary-deep);
  font-weight: 800;
  background: rgba(237, 244, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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

.sectionTitle {
  font-size: 16px;
  font-weight: 800;
}

.input,
.textarea {
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  font: inherit;
}

.textarea {
  min-height: 96px;
  resize: vertical;
}

.input {
  min-height: 54px;
}

@media (max-width: 1100px) {
  .cards {
    grid-template-columns: 1fr;
  }

  .detailGrid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .cardHead,
  .footer,
  .actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary,
  .metaGrid,
  .detailGrid,
  .logRow {
    grid-template-columns: 1fr;
  }

  .actions,
  .actions :global(button) {
    width: 100%;
  }

  .actions :global(button) {
    width: 100%;
  }

  .detailPanel {
    padding: 18px;
  }
}
</style>
