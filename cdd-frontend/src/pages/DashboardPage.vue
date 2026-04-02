<template>
  <WorkspaceLayout
    eyebrow="Console"
    title="商家后台首页"
    description="把待办、经营数据、趋势和快捷动作放在同一屏，优先看见最需要处理的事情。"
  >
    <section :class="$style.toolbar">
      <div :class="$style.toolbarMain">
        <div :class="$style.sectionEyebrow">经营总览</div>
        <h3 :class="$style.toolbarTitle">今日经营、待办与快捷动作集中查看</h3>
        <div :class="$style.toolbarMeta">
          <span>商家：{{ authStore.user.merchantName }}</span>
          <span>状态：{{ dashboardMode === 'remote' ? '数据正常' : '异常待排查' }}</span>
          <span>待办：{{ taskItems.length }} 项</span>
          <span>快捷动作：{{ visibleQuickActions.length }} 项</span>
        </div>
      </div>
      <div :class="$style.toolbarActions">
        <UiButton v-if="canViewOrders" variant="secondary" size="md" @click="void router.push('/orders')">查看订单</UiButton>
        <UiButton v-if="canCreateProduct" size="md" @click="void router.push('/products')">新增商品</UiButton>
      </div>
    </section>

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

    <section :class="$style.summaryGrid">
      <UiCard
        v-for="card in dashboardSummaryCards"
        :key="card.label"
        elevated
        :class="$style.summaryCard"
      >
        <div :class="$style.summaryLabel">{{ card.label }}</div>
        <div :class="$style.summaryValue">{{ card.value }}</div>
        <div :class="[$style.summaryMeta, $style[card.tone]]">{{ card.meta }}</div>
      </UiCard>
    </section>

    <section :class="$style.heroGrid">
      <UiCard elevated :class="$style.heroCard">
        <div :class="$style.heroMeta">今日概览</div>
        <div :class="$style.heroTitleRow">
          <div :class="$style.heroContent">
            <h2 :class="$style.heroTitle">{{ authStore.user.merchantName }}</h2>
            <p :class="$style.heroDescription">
              欢迎回来，当前工作台状态为
              {{ dashboardMode === 'remote' ? '数据正常' : '异常待排查' }}。
            </p>
          </div>
          <UiTag :tone="dashboardMode === 'remote' ? 'primary' : 'info'">
            {{ dashboardMode === 'remote' ? '经营中' : '待修复' }}
          </UiTag>
        </div>
        <div :class="$style.heroActions">
          <UiButton v-if="canCreateProduct" size="md" leading="+" @click="void router.push('/products')">新增商品</UiButton>
          <UiButton v-if="canViewConfig" variant="secondary" size="md" @click="void router.push('/config')">配置中心</UiButton>
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
          <UiStatePanel
            v-if="!taskItems.length"
            tone="empty"
            title="当前没有待办事项"
            description="当前没有需要优先处理的待办事项。"
          />
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
      <UiStatePanel
        v-if="!metricItems.length"
        tone="empty"
        title="暂无经营指标"
        description="请先准备日报数据，随后首页会展示经营指标。"
      />
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
        <div :class="$style.actionGrid">
          <button
            v-for="action in visibleQuickActions"
            :key="action"
            :class="$style.actionButton"
            :disabled="quickActionPending === action"
            @click="handleQuickAction(action)"
          >
            <span :class="$style.actionButtonTitle">
              {{ quickActionPending === action ? `${action}处理中...` : action }}
            </span>
            <span :class="$style.actionButtonText">
              {{ describeQuickAction(action) }}
            </span>
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
import { useAuthStore } from '@/stores/auth';
import {
  buildDashboardTrendOption,
  quickActions,
} from '@/modules/dashboard/state';
import type { DashboardMetricItem, DashboardTaskItem } from '@/modules/dashboard/state';
import type { MerchantDashboardSnapshotResponseRaw, ReportHomeEventDailyResponseRaw, ReportOrderDailyResponseRaw } from '@/types/report';
import type { EChartsOption } from 'echarts';

const authStore = useAuthStore();
const router = useRouter();
const dashboardMode = ref<'remote' | 'error'>('remote');
const dashboardNotice = ref('正在加载经营数据。');
const metricItems = ref<DashboardMetricItem[]>([]);
const taskItems = ref<DashboardTaskItem[]>([]);
const trendOption = ref<EChartsOption>(buildDashboardTrendOption([], []));
const quickActionNotice = ref('');
const quickActionTone = ref<'info' | 'error'>('info');
const quickActionPending = ref('');
const canViewOrders = computed(() => authStore.canAccess({ requiredModule: 'order', requiredAction: 'view' }));
const canCreateProduct = computed(() => authStore.canAccess({ requiredModule: 'product', requiredAction: 'edit' }));
const canViewConfig = computed(() => authStore.canAccess({ requiredModule: 'config', requiredAction: 'view' }));
const canExportOrders = computed(() => authStore.canAccess({ requiredModule: 'order', requiredAction: 'export' }));
const canOpenRelease = computed(() => authStore.canAccess({ requiredModule: 'release', requiredAction: 'view' }));
const canPublishConfig = computed(() => authStore.canAccess({ requiredModule: 'config', requiredAction: 'publish' }));
const visibleQuickActions = computed(() => quickActions.filter((action) => {
  if (action === '新增商品') {
    return canCreateProduct.value;
  }
  if (action === '进入发布治理') {
    return canOpenRelease.value;
  }
  if (action === '导出订单') {
    return canExportOrders.value;
  }
  if (action === '同步配置') {
    return canPublishConfig.value;
  }
  return false;
}));

const dashboardSummaryCards = computed(() => [
  {
    label: '当前状态',
    value: dashboardMode.value === 'remote' ? '数据正常' : '异常待排查',
    meta: dashboardMode.value === 'remote' ? '数据已更新' : dashboardNotice.value,
    tone: dashboardMode.value === 'remote' ? 'primary' : 'danger',
  },
  {
    label: '待办事项',
    value: String(taskItems.value.length),
    meta: taskItems.value.length ? taskItems.value[0].title : '当前没有待处理任务',
    tone: taskItems.value.length ? 'info' : 'success',
  },
  {
    label: '经营指标',
    value: String(metricItems.value.length),
    meta: metricItems.value.length ? '首页指标已加载' : '等待经营指标更新',
    tone: metricItems.value.length ? 'success' : 'default',
  },
  {
    label: '快捷动作',
    value: String(visibleQuickActions.value.length),
    meta: quickActionPending.value ? `${quickActionPending.value}执行中` : '支持当前账号可访问的快捷入口',
    tone: quickActionPending.value ? 'primary' : 'default',
  },
]);

const dashboardStatePanel = computed(() => {
  if (!authStore.businessScope.derivedFromContext) {
    return {
      tone: 'error' as const,
      title: '工作台缺少业务上下文',
      description: '鉴权上下文中的 merchant_id / store_id 不是数值类型，当前无法继续查询经营数据。',
    };
  }

  if (dashboardMode.value === 'error') {
    return {
      tone: 'error' as const,
      title: '经营数据加载失败',
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

function describeQuickAction(action: string): string {
  if (action === '新增商品') {
    return '进入商品管理，直接创建并维护商品信息。';
  }
  if (action === '进入发布治理') {
    return '进入发布治理页，确认模板版本后再创建发布任务。';
  }
  if (action === '导出订单') {
    return '导出当前商家订单数据。';
  }
  if (action === '同步配置') {
    return '创建配置发布记录并跳转到配置中心查看详情。';
  }
  return '执行当前快捷操作。';
}

function canUseQuickAction(action: string): boolean {
  return visibleQuickActions.value.includes(action);
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

async function openReleaseGovernanceFromDashboard() {
  const storeId = authStore.storeIdForQuery;
  const miniProgramId = parseNumericTail(authStore.context?.miniProgramId) ?? storeId;
  if (!storeId || !miniProgramId) {
    throw new Error('当前缺少小程序发布上下文，无法进入发布治理。');
  }

  setQuickActionNotice('已进入发布治理，请确认模板版本后再创建发布任务。');
  await router.push({
    path: '/releases',
    query: {
      mini_program_id: String(miniProgramId),
      trigger_source: 'dashboard_manual',
    },
  });
}

async function handleQuickAction(action: string) {
  try {
    if (!canUseQuickAction(action)) {
      throw new Error('当前账号没有执行该快捷动作的权限。');
    }
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
    if (action === '进入发布治理') {
      await openReleaseGovernanceFromDashboard();
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
    clearDashboard('业务上下文未准备完成，无法查询经营数据。');
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
      clearDashboard('当前还没有足够的工作台日报数据。');
      return;
    }

    dashboardMode.value = 'remote';
    dashboardNotice.value = '经营数据已更新。';
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
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.toolbarMain {
  display: grid;
  gap: 10px;
}

.toolbarTitle {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.toolbarMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.toolbarActions {
  display: flex;
  gap: 10px;
}

.summaryGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summaryCard {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.summaryLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.summaryValue {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.3;
}

.summaryMeta {
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.heroGrid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.8fr);
  gap: 20px;
}

.heroCard {
  padding: 20px;
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
  margin-top: 16px;
}

.heroContent {
  min-width: 0;
}

.heroTitle {
  margin: 0;
  font-size: 30px;
  line-height: 1.05;
  letter-spacing: -0.05em;
}

.heroDescription {
  max-width: 560px;
  margin: 12px 0 0;
  color: var(--cdd-text-soft);
  font-size: 14px;
  line-height: 1.7;
}

.heroActions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.taskPanel {
  padding: 18px;
}

.panelHeading {
  font-size: 16px;
  font-weight: 800;
}

.taskList {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.taskItem {
  padding: 14px;
  border-radius: 16px;
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
  font-size: 14px;
  font-weight: 800;
}

.taskDetail {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.metricGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.metricCard {
  padding: 14px;
}

.metricLabel {
  color: var(--cdd-text-faint);
  font-size: 13px;
  font-weight: 700;
}

.metricValue {
  margin-top: 14px;
  font-size: 28px;
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
  gap: 20px;
}

.chartCard,
.actionCard {
  padding: 18px;
}

.sectionHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.actionButton {
  min-height: 62px;
  display: grid;
  gap: 6px;
  align-content: center;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(248, 250, 253, 0.96), rgba(255, 255, 255, 0.98));
  color: var(--cdd-text);
  font-size: 13px;
  font-weight: 800;
  text-align: left;
  padding: 12px 14px;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.actionButtonTitle {
  font-size: 14px;
  font-weight: 800;
}

.actionButtonText {
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.6;
  font-weight: 600;
}

.actionButton:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 107, 0, 0.16);
  box-shadow: 0 8px 16px rgba(9, 29, 46, 0.08);
}

@media (max-width: 960px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .summaryGrid,
  .heroGrid,
  .contentGrid {
    grid-template-columns: 1fr;
  }

  .actionGrid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .toolbar,
  .heroCard,
  .taskPanel,
  .chartCard,
  .actionCard {
    padding: 18px;
  }

  .toolbarMeta,
  .toolbarActions,
  .heroTitleRow,
  .heroActions,
  .sectionHeader {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbarActions :global(button) {
    width: 100%;
  }

  .heroActions :global(button),
  .actionGrid button {
    width: 100%;
  }
}
</style>
