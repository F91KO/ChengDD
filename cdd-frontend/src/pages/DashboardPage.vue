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

    <section :class="$style.heroGrid">
      <UiCard elevated :class="$style.heroCard">
        <div :class="$style.heroMeta">今日概览</div>
        <div :class="$style.heroTitleRow">
          <div>
            <h2 :class="$style.heroTitle">{{ authStore.user.merchantName }}</h2>
            <p :class="$style.heroDescription">
              欢迎回来，当前工作台运行模式为 {{ dashboardMode === 'remote' ? '实时报表' : '演示模式' }}。
            </p>
          </div>
          <UiTag :tone="dashboardMode === 'remote' ? 'primary' : 'info'">
            {{ dashboardMode === 'remote' ? '经营中' : '演示数据' }}
          </UiTag>
        </div>
        <div :class="$style.heroActions">
          <UiButton size="lg" leading="+">新增商品</UiButton>
          <UiButton variant="secondary" size="lg">发布模板</UiButton>
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
          :tone="dashboardMode === 'remote' ? 'info' : 'loading'"
          :title="dashboardMode === 'remote' ? '已接入真实报表接口' : '当前使用演示工作台数据'"
          :description="
            dashboardMode === 'remote'
              ? '工作台指标和趋势已接入 report-service，并基于本地 MySQL 报表数据返回。'
              : dashboardNotice
          "
        />
        <div :class="$style.actionGrid">
          <button v-for="action in quickActions" :key="action" :class="$style.actionButton">
            {{ action }}
          </button>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import TrendChart from '@/components/charts/TrendChart.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { fetchHomeEventDailyList, fetchMerchantDashboardSnapshot, fetchOrderDailyList } from '@/services/report';
import { useAuthStore } from '@/stores/auth';
import {
  buildDashboardTrendOption,
  dashboardMetrics,
  dashboardTasks,
  dashboardTrendOption as mockDashboardTrendOption,
  quickActions,
} from '@/modules/dashboard/mock';
import type { DashboardMetricItem, DashboardTaskItem } from '@/modules/dashboard/mock';
import type { MerchantDashboardSnapshotResponseRaw, ReportHomeEventDailyResponseRaw, ReportOrderDailyResponseRaw } from '@/types/report';
import type { EChartsOption } from 'echarts';

const authStore = useAuthStore();
const dashboardMode = ref<'remote' | 'mock'>('mock');
const dashboardNotice = ref('正在尝试连接真实报表接口，未接通时会回退到演示数据。');
const metricItems = ref<DashboardMetricItem[]>(dashboardMetrics);
const taskItems = ref<DashboardTaskItem[]>(dashboardTasks);
const trendOption = ref<EChartsOption>(mockDashboardTrendOption);

const dashboardStatePanel = computed(() => {
  if (authStore.authMode === 'mock') {
    return {
      tone: 'empty' as const,
      title: '当前展示演示数据',
      description: '认证服务未接通，工作台指标和业务清单仍使用演示数据。',
    };
  }

  if (!authStore.businessScope.derivedFromContext) {
    return {
      tone: 'info' as const,
      title: '当前使用回退业务上下文',
      description: '鉴权上下文中的 merchant_id / store_id 不是数值型，工作台将使用默认测试上下文联调。',
    };
  }

  if (dashboardMode.value !== 'remote') {
    return {
      tone: 'info' as const,
      title: '报表接口暂未接通',
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

function defaultTaskItems(): DashboardTaskItem[] {
  return dashboardTasks.map((item) => ({ ...item }));
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

  return defaultTaskItems();
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
    return mockDashboardTrendOption;
  }
  return buildDashboardTrendOption(
    sorted.map((item) => formatShortDate(item.stat_date)),
    sorted.map((item) => Number((formatNumber(item.gross_amount) / 10000).toFixed(2))),
  );
}

function fallbackToMock(message: string) {
  dashboardMode.value = 'mock';
  dashboardNotice.value = message;
  metricItems.value = dashboardMetrics.map((item) => ({ ...item }));
  taskItems.value = defaultTaskItems();
  trendOption.value = mockDashboardTrendOption;
}

async function loadDashboard() {
  await authStore.ensureCurrentContext();

  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;
  if (!merchantId || !storeId) {
    fallbackToMock('业务上下文未准备完成，工作台暂时回退到演示数据。');
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
      fallbackToMock('报表链路已接通，但本地数据库还没有完整的工作台日报数据，当前回退到演示数据。');
      return;
    }

    dashboardMode.value = 'remote';
    dashboardNotice.value = '工作台已连接真实报表接口。';
    metricItems.value = toMetricItems(snapshot, homeEvents, orderSeries);
    taskItems.value = toTaskItems(snapshot);
    trendOption.value = toTrendOption(orderSeries);
  } catch (error) {
    fallbackToMock(`报表服务未就绪，工作台已回退到演示数据。${error instanceof Error ? error.message : ''}`.trim());
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
  gap: 16px;
  margin-top: 22px;
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
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

@media (max-width: 1200px) {
  .metricGrid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
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

  .metricGrid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
