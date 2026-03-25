<template>
  <WorkspaceLayout
    eyebrow="Console"
    title="商家后台首页"
    description="把待办、经营数据、趋势和快捷动作放在同一屏，优先看见最需要处理的事情。"
  >
    <UiStatePanel
      v-if="dashboardStatePanel"
      :tone="dashboardStatePanel.tone"
      :title="dashboardStatePanel.title"
      :description="dashboardStatePanel.description"
    />
    <UiStatePanel
      v-if="quickActionNotice"
      :tone="quickActionTone"
      title="快捷动作结果"
      :description="quickActionNotice"
    />

    <section :class="$style.heroGrid">
      <UiCard elevated :class="$style.heroCard">
        <div :class="$style.heroMeta">今日概览</div>
        <div :class="$style.heroTitleRow">
          <div :class="$style.heroContent">
            <h2 :class="$style.heroTitle">{{ authStore.user.merchantName }}</h2>
            <p :class="$style.heroDescription">
              欢迎回来，当前工作台运行模式为
              {{ dashboardMode === 'remote' ? '实时报表' : '异常待排查' }}。
            </p>
          </div>
          <UiTag :tone="dashboardMode === 'remote' ? 'primary' : 'info'">
            {{ dashboardMode === 'remote' ? '经营中' : '待修复' }}
          </UiTag>
        </div>
        <div :class="$style.heroActions">
          <UiButton size="lg" leading="+" @click="void router.push('/products')">新增商品</UiButton>
          <UiButton variant="secondary" size="lg" @click="void router.push('/config')">配置中心</UiButton>
        </div>
      </UiCard>

      <UiCard elevated :class="$style.taskPanel">
        <div :class="$style.panelHeading">待办事项</div>
        <div :class="$style.taskList">
          <article
            v-for="task in taskItems"
            :key="task.title"
            :class="[$style.taskItem, $style[task.tone]]"
          >
            <div :class="$style.taskTitle">{{ task.title }}</div>
            <div :class="$style.taskDetail">{{ task.detail }}</div>
          </article>
        </div>
      </UiCard>
    </section>

    <section :class="$style.metricGrid">
      <UiCard
        v-for="metric in metricItems"
        :key="metric.label"
        elevated
        :class="$style.metricCard"
      >
        <div :class="$style.metricLabel">{{ metric.label }}</div>
        <div :class="$style.metricValue">{{ metric.value }}</div>
        <div :class="[$style.metricDelta, $style[metric.tone]]">{{ metric.delta }}</div>
      </UiCard>
    </section>

    <section :class="$style.contentGrid">
      <UiCard elevated :class="$style.chartCard">
        <div :class="$style.sectionHeader">
          <div>
            <div :class="$style.sectionEyebrow">趋势</div>
            <h3 :class="$style.sectionTitle">近 7 日销售走势</h3>
          </div>
          <UiTag tone="info">营收单位 / 万元</UiTag>
        </div>
        <TrendChart :option="trendOption" />
      </UiCard>

      <UiCard elevated :class="$style.actionCard">
        <div :class="$style.sectionHeader">
          <div>
            <div :class="$style.sectionEyebrow">操作</div>
            <h3 :class="$style.sectionTitle">快捷动作</h3>
          </div>
        </div>
        <UiStatePanel
          :tone="dashboardMode === 'remote' ? 'info' : 'error'"
          :title="dashboardMode === 'remote' ? '已接入真实报表接口' : '报表链路异常'"
          :description="
            dashboardMode === 'remote'
              ? '工作台指标和趋势已接入 report-service，并基于本地 MySQL 报表数据返回。'
              : dashboardNotice
          "
        />
        <div :class="$style.actionGrid">
          <button
            v-for="action in quickActions"
            :key="action"
            :class="$style.actionButton"
            :disabled="quickActionPending === action"
            @click="handleQuickAction(action)"
          >
            {{ quickActionPending === action ? `${action}处理中...` : action }}
          </button>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import TrendChart from '@/components/charts/TrendChart.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { createPublishRecord } from '@/services/config';
import { exportOrdersCsv } from '@/services/order';
import { fetchHomeEventDailyList, fetchMerchantDashboardSnapshot, fetchOrderDailyList } from '@/services/report';
import { createReleaseTask } from '@/services/release';
import { useAuthStore } from '@/stores/auth';
import {
  buildDashboardTrendOption,
  quickActions,
} from '@/modules/dashboard/mock';
import type { DashboardMetricItem, DashboardTaskItem } from '@/modules/dashboard/mock';
import type { MerchantDashboardSnapshotResponseRaw, ReportHomeEventDailyResponseRaw, ReportOrderDailyResponseRaw } from '@/types/report';
import type { EChartsOption } from 'echarts';

const authStore = useAuthStore();
const router = useRouter();
const dashboardMode = ref<'remote' | 'error'>('remote');
const dashboardNotice = ref('正在加载真实报表接口。');
const metricItems = ref<DashboardMetricItem[]>([]);
const taskItems = ref<DashboardTaskItem[]>([]);
const trendOption = ref<EChartsOption>(buildDashboardTrendOption([], []));
const quickActionNotice = ref('');
const quickActionTone = ref<'info' | 'error'>('info');
const quickActionPending = ref('');

const dashboardStatePanel = computed(() => {
  if (!authStore.businessScope.derivedFromContext) {
    return {
      tone: 'error' as const,
      title: '工作台缺少真实业务上下文',
      description: '鉴权上下文中的 merchant_id / store_id 不是数值类型，当前无法继续查询真实报表。',
    };
  }

  if (dashboardMode.value === 'error') {
    return {
      tone: 'error' as const,
      title: '报表接口调用失败',
      description: dashboardNotice.value,
    };
  }

  return null;
});

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}

function formatShortDate(raw: string): string {
  const date = new Date(`${raw}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return raw;
  }
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function formatNumber(value: unknown, fallback = 0): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function toTaskItems(snapshot: MerchantDashboardSnapshotResponseRaw): DashboardTaskItem[] {
  const payload = snapshot.dashboard_payload;
  const todoSummary = payload.todo_summary;
  if (Array.isArray(todoSummary)) {
    const tasks = todoSummary
      .map((item) => {
        if (!item || typeof item !== 'object') {
          return null;
        }
        const candidate = item as Record<string, unknown>;
        const title = typeof candidate.title === 'string' ? candidate.title : '';
        const detail = typeof candidate.detail === 'string' ? candidate.detail : '';
        const tone = candidate.tone === 'danger' || candidate.tone === 'info' ? candidate.tone : 'default';
        if (!title || !detail) {
          return null;
        }
        return {
          title,
          detail,
          tone,
        } satisfies DashboardTaskItem;
      })
      .filter((item): item is DashboardTaskItem => item !== null);

    if (tasks.length > 0) {
      return tasks;
    }
  }

  return [];
}

function toMetricItems(
  snapshot: MerchantDashboardSnapshotResponseRaw,
  homeEvents: ReportHomeEventDailyResponseRaw[],
  orderSeries: ReportOrderDailyResponseRaw[],
): DashboardMetricItem[] {
  const payload = snapshot.dashboard_payload;
  const latestHomeEvent = homeEvents[0];
  const latestOrder = orderSeries[0];
  const latestGrossAmount = latestOrder ? formatNumber(latestOrder.gross_amount) : 0;
  const latestOrderCount = latestOrder ? formatNumber(latestOrder.order_count) : 0;
  const paidOrderCount = latestOrder ? formatNumber(latestOrder.paid_order_count) : 0;
  const pendingDeliveryCount = formatNumber(payload.pending_delivery_count);
  const afterSaleCount = formatNumber(payload.after_sale_processing_count);
  const activeProductCount = formatNumber(payload.active_product_count);
  const releaseExceptionCount = formatNumber(payload.release_exception_count);

  return [
    { label: '今日订单', value: String(latestOrderCount), delta: `已支付 ${paidOrderCount}`, tone: 'primary' },
    { label: '今日营收', value: formatCurrency(latestGrossAmount), delta: '来自日报汇总', tone: 'primary' },
    { label: '首页访客', value: String(latestHomeEvent?.visitor_count ?? 0), delta: `浏览 ${latestHomeEvent?.page_view_count ?? 0}`, tone: 'success' },
    { label: '待发货', value: String(pendingDeliveryCount), delta: '待仓配处理', tone: 'default' },
    { label: '售后处理中', value: String(afterSaleCount), delta: '需复核', tone: 'danger' },
    { label: '在售商品', value: String(activeProductCount), delta: `发布异常 ${releaseExceptionCount}`, tone: releaseExceptionCount > 0 ? 'danger' : 'success' },
  ];
}

function toTrendOption(orderSeries: ReportOrderDailyResponseRaw[]): EChartsOption {
  const sorted = [...orderSeries]
    .sort((left, right) => left.stat_date.localeCompare(right.stat_date))
    .slice(-7);
  if (sorted.length === 0) {
    return buildDashboardTrendOption([], []);
  }
  return buildDashboardTrendOption(
    sorted.map((item) => formatShortDate(item.stat_date)),
    sorted.map((item) => Number((formatNumber(item.gross_amount) / 10000).toFixed(2))),
  );
}

function clearDashboard(message: string, mode: 'remote' | 'error' = 'error') {
  dashboardMode.value = mode;
  dashboardNotice.value = message;
  metricItems.value = [];
  taskItems.value = [];
  trendOption.value = buildDashboardTrendOption([], []);
}

function setQuickActionNotice(message: string, tone: 'info' | 'error' = 'info') {
  quickActionNotice.value = message;
  quickActionTone.value = tone;
}

function parseNumericTail(raw: string | null | undefined): number | null {
  if (!raw) {
    return null;
  }

  const matched = raw.match(/(\d+)$/);
  if (!matched) {
    return null;
  }

  return Number(matched[1]);
}

async function downloadOrdersFromDashboard() {
  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;
  const userId = authStore.userIdForQuery;
  if (!merchantId || !storeId) {
    throw new Error('当前缺少商家或店铺上下文，无法导出订单。');
  }

  const blob = await exportOrdersCsv({
    merchantId,
    storeId,
    userId,
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `orders-${merchantId}-${storeId}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
  setQuickActionNotice('订单导出已开始，请查看浏览器下载列表。');
}

async function syncConfigFromDashboard() {
  const merchantId = authStore.context?.merchantId;
  const storeId = authStore.context?.storeId;
  if (!merchantId || !storeId) {
    throw new Error('当前缺少配置中心上下文，无法发起配置同步。');
  }

  const record = await createPublishRecord({
    merchantId,
    storeId,
    operatorName: authStore.user.operatorName || '商家管理员',
    publishNote: '工作台快捷动作发起配置同步',
  });
  setQuickActionNotice(`已创建配置同步记录 ${record.task_no}，正在跳转配置中心。`);
  await router.push('/config');
}

async function createReleaseFromDashboard() {
  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;
  const miniProgramId = parseNumericTail(authStore.context?.miniProgramId) ?? storeId;
  if (!merchantId || !storeId || !miniProgramId) {
    throw new Error('当前缺少小程序发布上下文，无法创建发布任务。');
  }

  const task = await createReleaseTask({
    merchantId,
    storeId,
    miniProgramId,
    templateVersionId: 9800001,
    releaseType: 'full_release',
    triggerSource: 'dashboard_manual',
    releaseSnapshotJson: JSON.stringify({
      source: 'dashboard_quick_action',
      operator_name: authStore.user.operatorName,
      merchant_id: merchantId,
      store_id: storeId,
    }),
  });
  setQuickActionNotice(`已创建发布任务 ${task.task_no}，当前状态 ${task.release_status}。`);
  await router.push({
    path: '/releases',
    query: {
      task_no: task.task_no,
    },
  });
}

async function handleQuickAction(action: string) {
  try {
    quickActionPending.value = action;
    if (action === '新增商品') {
      await router.push('/products');
      return;
    }
    if (action === '导出订单') {
      await downloadOrdersFromDashboard();
      return;
    }
    if (action === '同步配置') {
      await syncConfigFromDashboard();
      return;
    }
    if (action === '发布模板') {
      await createReleaseFromDashboard();
    }
  } catch (error) {
    setQuickActionNotice(error instanceof Error ? error.message : `${action}执行失败。`, 'error');
  } finally {
    quickActionPending.value = '';
  }
}

async function loadDashboard() {
  await authStore.ensureCurrentContext();

  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;
  if (!merchantId || !storeId) {
    clearDashboard('业务上下文未准备完成，无法查询真实报表。');
    return;
  }

  const endDate = new Date();
  const startDate = new Date();
  startDate.setDate(endDate.getDate() - 6);
  const startDateText = startDate.toISOString().slice(0, 10);
  const endDateText = endDate.toISOString().slice(0, 10);

  try {
    const [snapshot, homeEvents, orderSeries] = await Promise.all([
      fetchMerchantDashboardSnapshot({ merchantId, storeId }),
      fetchHomeEventDailyList({ merchantId, storeId, startDate: startDateText, endDate: endDateText }),
      fetchOrderDailyList({ merchantId, storeId, startDate: startDateText, endDate: endDateText }),
    ]);

    if (!homeEvents.length || !orderSeries.length) {
      clearDashboard('报表接口已接通，但当前数据库还没有足够的工作台日报数据。');
      return;
    }

    dashboardMode.value = 'remote';
    dashboardNotice.value = '工作台已连接真实报表接口。';
    metricItems.value = toMetricItems(snapshot, homeEvents, orderSeries);
    taskItems.value = toTaskItems(snapshot);
    trendOption.value = toTrendOption(orderSeries);
  } catch (error) {
    clearDashboard(error instanceof Error ? error.message : '报表服务未就绪。');
  }
}

onMounted(() => {
  void loadDashboard();
});
</script>

<style module>
.heroGrid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.8fr);
  gap: 24px;
}

.heroCard {
  padding: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(255, 107, 0, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 249, 255, 0.94));
}

.heroMeta {
  color: var(--cdd-primary-deep);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.heroTitleRow {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-top: 22px;
}

.heroContent {
  min-width: 0;
}

.heroTitle {
  margin: 0;
  font-size: 34px;
  line-height: 1.05;
  letter-spacing: -0.05em;
}

.heroDescription {
  max-width: 560px;
  margin: 14px 0 0;
  color: var(--cdd-text-soft);
  line-height: 1.8;
}

.heroActions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.taskPanel {
  padding: 24px;
}

.panelHeading {
  font-size: 18px;
  font-weight: 800;
}

.taskList {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.taskItem {
  padding: 18px;
  border-radius: 18px;
}

.taskItem.default {
  background: #eef2f8;
}

.taskItem.danger {
  background: var(--cdd-danger-soft);
}

.taskItem.info {
  background: var(--cdd-info-soft);
}

.taskTitle {
  font-size: 15px;
  font-weight: 800;
}

.taskDetail {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.metricGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.metricCard {
  padding: 18px;
}

.metricLabel {
  color: var(--cdd-text-faint);
  font-size: 13px;
  font-weight: 700;
}

.metricValue {
  margin-top: 18px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.metricDelta {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 800;
}

.metricDelta.primary {
  color: var(--cdd-primary);
}

.metricDelta.default {
  color: var(--cdd-text-soft);
}

.metricDelta.success {
  color: var(--cdd-success);
}

.metricDelta.danger {
  color: var(--cdd-danger);
}

.contentGrid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.8fr);
  gap: 24px;
}

.chartCard,
.actionCard {
  padding: 24px;
}

.sectionHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.sectionEyebrow {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sectionTitle {
  margin: 8px 0 0;
  font-size: 22px;
  letter-spacing: -0.04em;
}

.actionGrid {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.actionButton {
  min-height: 64px;
  border: 0;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(237, 244, 255, 0.88), rgba(255, 255, 255, 0.98));
  color: var(--cdd-text);
  font-size: 15px;
  font-weight: 800;
  text-align: left;
  padding: 0 18px;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.actionButton:hover {
  transform: translateY(-1px);
  box-shadow: var(--cdd-shadow-soft);
}

@media (max-width: 960px) {
  .heroGrid,
  .contentGrid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .heroCard,
  .taskPanel,
  .chartCard,
  .actionCard {
    padding: 18px;
  }

  .heroTitleRow,
  .heroActions,
  .sectionHeader {
    flex-direction: column;
    align-items: flex-start;
  }

  .heroActions :global(button),
  .actionGrid button {
    width: 100%;
  }
}
</style>
