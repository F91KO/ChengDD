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
              <UiButton variant="secondary">查看详情</UiButton>
              <UiButton v-if="record.tabStatus === '待处理'">快速同意</UiButton>
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
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { fetchAfterSaleList } from '@/services/aftersales';
import { useAuthStore } from '@/stores/auth';
import type { OrderAfterSaleSummaryResponseRaw } from '@/types/aftersales';
import { aftersalesRecords, aftersalesTabs } from '@/modules/aftersales/mock';

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
};

const authStore = useAuthStore();
const activeTab = ref('全部');
const afterSalesMode = ref<'remote' | 'mock'>('mock');
const afterSalesLoading = ref(true);
const afterSalesNotice = ref('正在尝试连接真实售后接口，未接通时会回退到演示数据。');

function toMockCardRecord(item: typeof aftersalesRecords[number]): AfterSaleCardRecord {
  return {
    ...item,
    tabStatus: item.status === '退款中' ? '已同意' : item.status,
    statusTone: item.statusTone as AfterSaleCardRecord['statusTone'],
  };
}

const records = ref<AfterSaleCardRecord[]>(aftersalesRecords.map(toMockCardRecord));

const afterSaleStatePanel = computed(() => {
  if (afterSalesLoading.value) {
    return {
      tone: 'loading' as const,
      title: '正在加载售后列表',
      description: '页面会优先请求真实售后接口，接口不可用时自动回退到演示数据。',
    };
  }

  if (afterSalesMode.value === 'remote') {
    return {
      tone: 'info' as const,
      title: '已接入真实售后列表接口',
      description: '售后列表已从 order-service 读取，详情页和快捷处理动作后续继续补齐。',
    };
  }

  if (authStore.authMode === 'mock') {
    return {
      tone: 'empty' as const,
      title: '当前展示演示售后数据',
      description: '认证服务未接通，售后列表暂时使用演示数据。',
    };
  }

  return {
    tone: 'info' as const,
    title: '售后接口暂未接通',
    description: afterSalesNotice.value,
  };
});

const filteredRecords = computed(() => {
  if (activeTab.value === '全部') {
    return records.value;
  }

  return records.value.filter((record) => record.tabStatus === activeTab.value);
});

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
  };
}

function fallbackToMock(message: string) {
  afterSalesMode.value = 'mock';
  afterSalesNotice.value = message;
  records.value = aftersalesRecords.map(toMockCardRecord);
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
    });
    if (response.length === 0) {
      fallbackToMock('真实售后接口已接通，但当前商家暂无可展示的售后单。');
      return;
    }
    afterSalesMode.value = 'remote';
    afterSalesNotice.value = '售后列表已连接真实接口。';
    records.value = response.map(toCardRecord);
  } catch (error) {
    fallbackToMock(`售后服务未就绪，页面已回退到演示数据。${error instanceof Error ? error.message : ''}`.trim());
  } finally {
    afterSalesLoading.value = false;
  }
}

onMounted(() => {
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
  .actions {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .actions {
    width: 100%;
  }
}
</style>
