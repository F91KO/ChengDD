<template>
  <WorkspaceLayout
    eyebrow="Orders"
    title="订单管理"
    description="查看订单列表、详情、物流发货和状态日志，减少在列表页与详情页之间来回切换。"
  >
    <section :class="$style.toolbar">
      <div :class="$style.toolbarMain">
        <div :class="$style.filters">
          <button
            v-for="filter in orderFilters"
            :key="filter"
            :class="[$style.filterChip, filter === activeFilter ? $style.filterChipActive : '']"
            @click="activeFilter = filter"
          >
            {{ filter }}
          </button>
        </div>
        <div :class="$style.toolbarMeta">
          <span>当前筛选：{{ activeFilter }}</span>
          <span>当前页 {{ orders.length }} 单</span>
          <span>总计 {{ orderPagination.total }} 单</span>
        </div>
      </div>
      <div :class="$style.toolbarActions">
        <UiButton variant="secondary" size="sm" :disabled="orderLoadState.loading || !orders.length" @click="handleExportOrders">批量导出</UiButton>
        <UiButton :disabled="orderLoadState.loading || !hasShippableOrders" @click="openDeliveryPanelFromList">快速发货</UiButton>
      </div>
    </section>

    <UiStatePanel
      v-if="orderLoadState.loading"
      tone="loading"
      title="正在加载订单列表"
      description="正在读取订单数据。"
    />
    <UiStatePanel
      v-else-if="orderLoadState.errorMessage"
      tone="error"
      title="订单列表加载失败"
      :description="orderLoadState.message"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.tableWrap">
      <UiCard elevated :class="$style.listSummaryCard">
        <div>
          <div :class="$style.detailEyebrow">订单列表</div>
          <div :class="$style.listSummaryTitle">当前筛选下的订单</div>
          <div :class="$style.listSummaryText">
            {{ activeFilter }} · 第 {{ orderPagination.page }} 页 · 每页 {{ orderPagination.pageSize }} 单
          </div>
        </div>
        <div :class="$style.listSummaryMeta">
          <span>总计 {{ orderPagination.total }} 单</span>
          <span>当前展示 {{ orders.length }} 单</span>
        </div>
      </UiCard>

      <UiCard elevated :class="$style.tableCard">
        <div :class="$style.tableHeader">
          <span>订单号</span>
          <span>客户</span>
          <span>渠道</span>
          <span>金额</span>
          <span>状态</span>
          <span>时间</span>
          <span>操作</span>
        </div>

        <article
          v-for="order in orders"
          v-show="!orderLoadState.loading"
          :key="order.orderNo"
          :class="$style.row"
        >
          <div :class="$style.cell">
            <div :class="$style.cellLabel">订单号</div>
            <div :class="$style.mainText">{{ order.orderNo }}</div>
            <div :class="$style.subText">{{ order.items }}</div>
          </div>

          <div :class="$style.cell">
            <div :class="$style.cellLabel">客户</div>
            <div :class="$style.mainText">{{ order.customer }}</div>
          </div>

          <div :class="$style.cell">
            <div :class="$style.cellLabel">渠道</div>
            <div :class="$style.subText">{{ order.channel }}</div>
          </div>

          <div :class="$style.cell">
            <div :class="$style.cellLabel">金额</div>
            <div :class="$style.mainText">{{ order.amount }}</div>
          </div>

          <div :class="$style.cell">
            <div :class="$style.cellLabel">状态</div>
            <UiTag :tone="order.statusTone as 'primary' | 'info' | 'danger' | 'success'">
              {{ order.status }}
            </UiTag>
          </div>

          <div :class="$style.cell">
            <div :class="$style.cellLabel">时间</div>
            <div :class="$style.subText">{{ order.time }}</div>
          </div>

          <div :class="[$style.cell, $style.actionCell]">
            <div :class="$style.cellLabel">操作</div>
            <div :class="$style.rowActions">
              <UiButton variant="secondary" size="sm" @click="handleViewDetail(order)">查看详情</UiButton>
              <UiButton
                v-if="canShipOrder(order)"
                variant="secondary"
                size="sm"
                @click="openDeliveryPanel(order)"
              >
                {{ getShipActionLabel(order) }}
              </UiButton>
            </div>
          </div>
        </article>

        <UiStatePanel
          v-if="!orderLoadState.loading && orders.length === 0"
          tone="empty"
          title="当前筛选下没有订单"
          description="可以切换筛选条件，或稍后刷新后再查看。"
        >
          <UiButton variant="secondary" size="sm" @click="activeFilter = '全部'">查看全部订单</UiButton>
        </UiStatePanel>
      </UiCard>
    </section>

    <UiPagination
      :page="orderPagination.page"
      :page-size="orderPagination.pageSize"
      :total="orderPagination.total"
      :disabled="orderLoadState.loading"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <div v-if="selectedOrderDetail" ref="detailAnchor">
      <UiCard elevated :class="$style.detailPanel">
        <div :class="$style.detailHead">
          <div>
            <div :class="$style.detailEyebrow">订单详情</div>
            <div :class="$style.detailTitle">{{ selectedOrderDetail.order_no }}</div>
          </div>
          <div :class="$style.detailStatusGroup">
            <UiTag :tone="orderStatusTone(selectedOrderDetail.order_status)">
              {{ formatOrderStatus(selectedOrderDetail.order_status) }}
            </UiTag>
            <UiTag :tone="payStatusTone(selectedOrderDetail.pay_status)">
              {{ formatPayStatus(selectedOrderDetail.pay_status) }}
            </UiTag>
            <UiTag :tone="deliveryStatusTone(selectedOrderDetail.delivery_status)">
              {{ formatDeliveryStatus(selectedOrderDetail.delivery_status) }}
            </UiTag>
          </div>
          <UiButton variant="secondary" size="sm" @click="closeDetailPanel">关闭面板</UiButton>
        </div>

        <div :class="$style.detailTabs">
          <button
            v-for="tab in orderDetailTabs"
            :key="tab.value"
            type="button"
            :class="[$style.detailTab, detailTab === tab.value ? $style.detailTabActive : '']"
            @click="detailTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <template v-if="detailTab === 'overview'">
          <section :class="$style.overviewGrid">
            <article :class="$style.summaryCard">
              <div :class="$style.summaryLabel">订单状态</div>
              <div :class="$style.summaryValue">{{ formatOrderStatus(selectedOrderDetail.order_status) }}</div>
            </article>
            <article :class="$style.summaryCard">
              <div :class="$style.summaryLabel">支付状态</div>
              <div :class="$style.summaryValue">{{ formatPayStatus(selectedOrderDetail.pay_status) }}</div>
            </article>
            <article :class="$style.summaryCard">
              <div :class="$style.summaryLabel">物流状态</div>
              <div :class="$style.summaryValue">{{ formatDeliveryStatus(selectedOrderDetail.delivery_status) }}</div>
            </article>
            <article :class="$style.summaryCard">
              <div :class="$style.summaryLabel">下单时间</div>
              <div :class="$style.summaryValue">{{ formatDateTime(selectedOrderDetail.created_at) }}</div>
            </article>
          </section>

          <section :class="$style.infoGrid">
            <article :class="$style.infoCard">
              <div :class="$style.sectionTitle">收货信息</div>
              <div :class="$style.infoList">
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">收货人</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.receiver_name || '-' }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">联系电话</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.receiver_mobile || '-' }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">收货地址</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.receiver_address || '-' }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">买家备注</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.buyer_remark || '-' }}</div>
                </div>
              </div>
            </article>

            <article :class="$style.infoCard">
              <div :class="$style.sectionTitle">金额信息</div>
              <div :class="$style.infoList">
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">订单总额</div>
                  <div :class="$style.infoValue">{{ formatCurrency(selectedOrderDetail.total_amount) }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">优惠金额</div>
                  <div :class="$style.infoValue">{{ formatCurrency(selectedOrderDetail.discount_amount) }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">运费</div>
                  <div :class="$style.infoValue">{{ formatCurrency(selectedOrderDetail.delivery_fee_amount) }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">实付金额</div>
                  <div :class="$style.infoValue">{{ formatCurrency(selectedOrderDetail.paid_amount) }}</div>
                </div>
              </div>
            </article>

            <article :class="$style.infoCard">
              <div :class="$style.sectionTitle">物流信息</div>
              <div :class="$style.infoList">
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">物流公司编码</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.logistics_company_code || '-' }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">物流公司名称</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.logistics_company_name || '-' }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">物流单号</div>
                  <div :class="$style.infoValue">{{ selectedOrderDetail.tracking_no || '-' }}</div>
                </div>
                <div :class="$style.infoItem">
                  <div :class="$style.infoLabel">发货时间</div>
                  <div :class="$style.infoValue">{{ formatDateTime(selectedOrderDetail.shipped_at) }}</div>
                </div>
              </div>
            </article>
          </section>
        </template>

        <section v-else-if="detailTab === 'items'" :class="$style.sectionBlock">
          <div :class="$style.sectionTitle">商品明细</div>
          <div v-if="selectedOrderDetail.items.length" :class="$style.listWrap">
            <article v-for="item in selectedOrderDetail.items" :key="item.id" :class="$style.itemRow">
              <div :class="$style.itemMain">
                <div :class="$style.itemName">{{ item.product_name }}</div>
                <div :class="$style.itemSpec">{{ item.sku_name }}</div>
              </div>
              <div :class="$style.itemMeta">
                <div :class="$style.itemMetaLabel">数量</div>
                <div :class="$style.itemMetaValue">{{ item.quantity }} 件</div>
              </div>
              <div :class="$style.itemMeta">
                <div :class="$style.itemMetaLabel">金额</div>
                <div :class="$style.itemMetaValue">{{ formatCurrency(item.line_amount) }}</div>
              </div>
            </article>
          </div>
          <UiStatePanel
            v-else
            tone="empty"
            title="当前订单没有商品明细"
            description="可以先查看概览信息，或稍后刷新后再试。"
          />
        </section>

        <section v-else-if="detailTab === 'logs'" :class="$style.sectionBlock">
          <div :class="$style.sectionTitle">状态日志</div>
          <div v-if="selectedOrderDetail.status_logs.length" :class="$style.listWrap">
            <article
              v-for="log in selectedOrderDetail.status_logs"
              :key="`${log.operate_type}-${log.created_at}`"
              :class="$style.logRow"
            >
              <div :class="$style.logMain">
                <div :class="$style.itemName">{{ formatOperateType(log.operate_type) }}</div>
                <div :class="$style.itemSpec">
                  {{ formatOrderStatus(log.from_status) }} -> {{ formatOrderStatus(log.to_status) }}
                </div>
              </div>
              <div :class="$style.itemMeta">
                <div :class="$style.itemMetaLabel">操作人</div>
                <div :class="$style.itemMetaValue">{{ log.operator_name || '系统' }}</div>
              </div>
              <div :class="$style.itemMeta">
                <div :class="$style.itemMetaLabel">时间</div>
                <div :class="$style.itemMetaValue">{{ formatDateTime(log.created_at) }}</div>
              </div>
              <div v-if="log.remark" :class="$style.logRemark">
                {{ log.remark }}
              </div>
            </article>
          </div>
          <UiStatePanel
            v-else
            tone="empty"
            title="当前订单没有状态日志"
            description="可以先查看概览信息，或稍后刷新后再试。"
          />
        </section>

        <section v-else-if="deliveryTarget && deliveryAction" :class="$style.deliveryBlock">
          <div :class="$style.deliveryHead">
            <div>
              <div :class="$style.sectionTitle">发货操作</div>
              <div :class="$style.deliveryHint">
                当前将订单发货到 {{ deliveryAction.targetLabel }}
              </div>
            </div>
            <UiTag :tone="deliveryAction.tone">
              {{ deliveryAction.panelLabel }}
            </UiTag>
          </div>

          <div :class="$style.formGrid">
            <UiInput
              v-model="shipForm.logisticsCompanyCode"
              label="物流公司编码"
              placeholder="例如：SF"
            />
            <UiInput
              v-model="shipForm.logisticsCompanyName"
              label="物流公司名称"
              placeholder="例如：顺丰速运"
            />
            <UiInput
              v-model="shipForm.trackingNo"
              label="物流单号"
              placeholder="请输入运单号"
            />
          </div>

          <label :class="$style.fieldBlock">
            <span :class="$style.summaryLabel">发货备注</span>
            <textarea
              v-model="shipForm.shipRemark"
              :class="$style.textarea"
              placeholder="请输入发货备注，例如打包批次、交接说明或特殊配送要求"
            />
          </label>

          <div :class="$style.panelActions">
            <UiButton variant="secondary" size="sm" @click="resetShipForm">清空信息</UiButton>
            <UiButton :disabled="submitting || !canSubmitShip" @click="submitDelivery">
              {{ submitting ? '正在提交...' : deliveryAction.buttonLabel }}
            </UiButton>
          </div>
        </section>

        <UiStatePanel
          v-else
          tone="empty"
          title="当前没有待处理发货"
          description="该订单当前状态下无需发货处理，可切换到概览或日志查看详情。"
        />
      </UiCard>
    </div>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiPagination from '@/components/base/UiPagination.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { exportOrdersCsv, fetchOrderDetail, shipOrder } from '@/services/order';
import { useAuthStore } from '@/stores/auth';
import {
  filterToOrderStatus,
  loadOrders,
  orderFilters,
  orderLoadState,
  orderPagination,
  orders,
  type OrderCard,
} from '@/modules/orders/state';
import type { OrderDetailResponseRaw } from '@/types/order';

const authStore = useAuthStore();
const activeFilter = ref('全部');
const detailTab = ref<'overview' | 'items' | 'logs' | 'delivery'>('overview');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const selectedOrderDetail = ref<OrderDetailResponseRaw | null>(null);
const deliveryTarget = ref<OrderCard | null>(null);
const submitting = ref(false);
const detailAnchor = ref<HTMLElement | null>(null);
const shipForm = reactive({
  logisticsCompanyCode: '',
  logisticsCompanyName: '',
  trackingNo: '',
  shipRemark: '',
});
const orderDetailTabs = [
  { value: 'overview' as const, label: '订单概览' },
  { value: 'items' as const, label: '商品明细' },
  { value: 'logs' as const, label: '状态日志' },
  { value: 'delivery' as const, label: '发货操作' },
];

type DeliveryAction = {
  buttonLabel: string;
  panelLabel: string;
  targetLabel: string;
  tone: 'info';
};

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

function formatOrderStatus(status: string | null | undefined): string {
  const normalized = (status || '').toLowerCase();
  if (normalized.includes('pending_pay')) {
    return '待支付';
  }
  if (normalized.includes('pending_ship') || normalized.includes('paid') || normalized.includes('preparing')) {
    return '待发货';
  }
  if (normalized.includes('pending_receive') || normalized.includes('shipped')) {
    return '待收货';
  }
  if (normalized.includes('finished') || normalized.includes('complete')) {
    return '已完成';
  }
  if (normalized.includes('cancel')) {
    return '已取消';
  }
  return status || '-';
}

function orderStatusTone(status: string | null | undefined): 'default' | 'primary' | 'info' | 'success' | 'danger' {
  const normalized = (status || '').toLowerCase();
  if (normalized.includes('finished') || normalized.includes('complete')) {
    return 'success';
  }
  if (normalized.includes('pending_ship') || normalized.includes('paid') || normalized.includes('preparing')) {
    return 'primary';
  }
  if (normalized.includes('pending_receive') || normalized.includes('shipped')) {
    return 'info';
  }
  if (normalized.includes('cancel')) {
    return 'danger';
  }
  return 'default';
}

function formatPayStatus(status: string | null | undefined): string {
  const normalized = (status || '').toLowerCase();
  if (normalized === 'unpaid' || normalized.includes('unpaid')) {
    return '待支付';
  }
  if (normalized === 'paying' || normalized.includes('paying')) {
    return '支付中';
  }
  if (normalized.includes('paid') || normalized.includes('success')) {
    return '已支付';
  }
  if (normalized.includes('pending')) {
    return '待支付';
  }
  if (normalized.includes('refund')) {
    return '已退款';
  }
  if (normalized.includes('close') || normalized.includes('cancel')) {
    return '已关闭';
  }
  return status || '-';
}

function payStatusTone(status: string | null | undefined): 'default' | 'primary' | 'info' | 'success' | 'danger' {
  const normalized = (status || '').toLowerCase();
  if (normalized.includes('paid') || normalized.includes('success')) {
    return 'success';
  }
  if (normalized.includes('paying')) {
    return 'primary';
  }
  if (normalized.includes('refund')) {
    return 'info';
  }
  if (normalized.includes('close') || normalized.includes('cancel')) {
    return 'danger';
  }
  return 'default';
}

function formatDeliveryStatus(status: string | null | undefined): string {
  const normalized = (status || '').toLowerCase();
  if (normalized.includes('pending')) {
    return '待发货';
  }
  if (normalized.includes('delivered') || normalized.includes('received') || normalized.includes('signed')) {
    return '已妥投';
  }
  if (normalized.includes('ship') || normalized.includes('deliver')) {
    return '运输中';
  }
  if (normalized.includes('finish') || normalized.includes('complete')) {
    return '已完成';
  }
  return status || '-';
}

function deliveryStatusTone(status: string | null | undefined): 'default' | 'primary' | 'info' | 'success' | 'danger' {
  const normalized = (status || '').toLowerCase();
  if (normalized.includes('delivered') || normalized.includes('received') || normalized.includes('signed') || normalized.includes('finish') || normalized.includes('complete')) {
    return 'success';
  }
  if (normalized.includes('ship') || normalized.includes('deliver')) {
    return 'primary';
  }
  return 'default';
}

function formatOperateType(type: string | null | undefined): string {
  const normalized = (type || '').toLowerCase();
  if (normalized.includes('pay')) {
    return '支付状态变更';
  }
  if (normalized.includes('ship')) {
    return '订单发货';
  }
  if (normalized.includes('delivery')) {
    return '物流状态变更';
  }
  if (normalized.includes('cancel')) {
    return '订单取消';
  }
  if (normalized.includes('create')) {
    return '创建订单';
  }
  if (normalized.includes('system')) {
    return '系统处理';
  }
  return type || '状态更新';
}

function getDeliveryAction(order: Pick<OrderCard, 'statusRaw' | 'deliveryStatusRaw'>): DeliveryAction | null {
  const orderStatus = (order.statusRaw || '').toLowerCase();
  if (orderStatus.includes('pending_ship') || orderStatus.includes('paid') || orderStatus.includes('preparing')) {
    return {
      buttonLabel: '确认发货',
      panelLabel: '物流发货',
      targetLabel: '待收货',
      tone: 'info',
    };
  }
  return null;
}

function canShipOrder(order: Pick<OrderCard, 'statusRaw' | 'deliveryStatusRaw'>): boolean {
  return getDeliveryAction(order) !== null;
}

function getShipActionLabel(order: Pick<OrderCard, 'statusRaw' | 'deliveryStatusRaw'>): string {
  return getDeliveryAction(order)?.buttonLabel ?? '';
}

const deliveryAction = computed(() => {
  if (!deliveryTarget.value) {
    return null;
  }
  return getDeliveryAction(deliveryTarget.value);
});
const hasShippableOrders = computed(() => orders.some((item) => canShipOrder(item)));

const canSubmitShip = computed(() => (
  shipForm.logisticsCompanyCode.trim().length > 0
  && shipForm.logisticsCompanyName.trim().length > 0
  && shipForm.trackingNo.trim().length > 0
));

function resetShipForm() {
  shipForm.logisticsCompanyCode = '';
  shipForm.logisticsCompanyName = '';
  shipForm.trackingNo = '';
  shipForm.shipRemark = '';
}

async function refreshOrders() {
  await loadOrders(true, filterToOrderStatus(activeFilter.value), orderPagination.page, orderPagination.pageSize);
}

async function loadOrderDetailOrThrow(order: OrderCard): Promise<OrderDetailResponseRaw> {
  return fetchOrderDetail({
    orderNo: order.orderNo,
    merchantId: order.merchantId,
    storeId: order.storeId,
    userId: order.userId,
  });
}

function handlePageChange(page: number) {
  void loadOrders(true, filterToOrderStatus(activeFilter.value), page, orderPagination.pageSize);
}

function handlePageSizeChange(pageSize: number) {
  void loadOrders(true, filterToOrderStatus(activeFilter.value), 1, pageSize);
}

async function handleViewDetail(order: OrderCard) {
  try {
    selectedOrderDetail.value = await loadOrderDetailOrThrow(order);
    deliveryTarget.value = null;
    detailTab.value = 'overview';
    scrollDetailIntoView();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '查询订单详情失败。', 'error');
  }
}

async function openDeliveryPanel(order: OrderCard) {
  try {
    selectedOrderDetail.value = await loadOrderDetailOrThrow(order);
    deliveryTarget.value = order;
    detailTab.value = 'delivery';
    shipForm.logisticsCompanyCode = selectedOrderDetail.value?.logistics_company_code ?? '';
    shipForm.logisticsCompanyName = selectedOrderDetail.value?.logistics_company_name ?? '';
    shipForm.trackingNo = selectedOrderDetail.value?.tracking_no ?? '';
    shipForm.shipRemark = '';
    scrollDetailIntoView();
  } catch (error) {
    deliveryTarget.value = null;
    setActionMessage(error instanceof Error ? error.message : '发货面板打开失败。', 'error');
  }
}

async function submitDelivery() {
  if (!deliveryTarget.value || !deliveryAction.value) {
    return;
  }

  try {
    submitting.value = true;
    const action = deliveryAction.value;
    const orderNo = deliveryTarget.value.orderNo;
    const operatorId = authStore.userIdForQuery;
    if (!operatorId) {
      throw new Error('当前上下文缺少操作人 ID。');
    }
    await shipOrder({
      orderNo,
      merchantId: deliveryTarget.value.merchantId,
      storeId: deliveryTarget.value.storeId,
      userId: deliveryTarget.value.userId,
      operatorId,
      logisticsCompanyCode: shipForm.logisticsCompanyCode.trim(),
      logisticsCompanyName: shipForm.logisticsCompanyName.trim(),
      trackingNo: shipForm.trackingNo.trim(),
      shipRemark: shipForm.shipRemark.trim(),
    });
    await refreshOrders();

    const refreshedTarget = orders.find((item) => item.orderNo === orderNo) ?? deliveryTarget.value;
    await handleViewDetail(refreshedTarget);
    deliveryTarget.value = refreshedTarget;
    setActionMessage(`订单 ${orderNo} 已完成发货，当前状态为 ${action.targetLabel}。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '订单发货失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

function openDeliveryPanelFromList() {
  const target = orders.find((item) => canShipOrder(item));
  if (!target) {
    setActionMessage('当前列表没有可发货的订单。', 'error');
    return;
  }
  void openDeliveryPanel(target);
}

async function handleExportOrders() {
  try {
    await authStore.ensureCurrentContext();
    const merchantId = authStore.merchantIdForQuery;
    const storeId = authStore.storeIdForQuery;
    if (!merchantId || !storeId) {
      throw new Error('当前缺少商家或店铺上下文，无法导出订单。');
    }

    const blob = await exportOrdersCsv({
      merchantId,
      storeId,
      userId: authStore.userIdForQuery,
      orderStatus: filterToOrderStatus(activeFilter.value),
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'orders-export.csv';
    anchor.click();
    URL.revokeObjectURL(url);
    setActionMessage('订单导出已开始，请查看浏览器下载列表。');
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '订单导出失败。', 'error');
  }
}

function closeDetailPanel() {
  selectedOrderDetail.value = null;
  deliveryTarget.value = null;
  detailTab.value = 'overview';
  resetShipForm();
}

onMounted(() => {
  void loadOrders(false, filterToOrderStatus(activeFilter.value), orderPagination.page, orderPagination.pageSize);
});

watch(activeFilter, () => {
  void loadOrders(true, filterToOrderStatus(activeFilter.value), 1, orderPagination.pageSize);
});
</script>

<style module>
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.toolbarMain {
  display: grid;
  gap: 12px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.filterChip {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 14px;
  color: var(--cdd-text-soft);
  background: rgba(248, 250, 253, 0.96);
  font-size: 13px;
  font-weight: 800;
}

.filterChipActive {
  color: #9c4304;
  border-color: rgba(255, 107, 0, 0.22);
  background: linear-gradient(135deg, rgba(255, 107, 0, 0.1), rgba(255, 249, 244, 0.98));
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.08);
}

.toolbarActions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
}

.toolbarMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.listSummaryCard {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
  padding: 18px;
}

.listSummaryTitle {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.listSummaryText,
.listSummaryMeta {
  margin-top: 10px;
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.listSummaryMeta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px 14px;
  font-size: 12px;
  font-weight: 700;
}

.formGrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.tableWrap {
  min-width: 0;
}

.tableCard {
  display: grid;
  gap: 0;
  overflow: hidden;
}

.tableHeader,
.row {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr 1fr 0.8fr 0.8fr 1fr 1.2fr;
  gap: 14px;
  align-items: start;
  padding: 14px 16px;
}

.tableHeader {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(237, 244, 255, 0.82);
}

.row + .row {
  box-shadow: inset 0 1px 0 rgba(9, 29, 46, 0.04);
}

.cell {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.cellLabel {
  display: none;
  color: var(--cdd-text-faint);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.actionCell {
  align-content: start;
}

.mainText {
  font-size: 13px;
  font-weight: 800;
}

.subText {
  color: var(--cdd-text-soft);
  font-size: 13px;
}

.rowActions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detailPanel {
  margin-top: 16px;
  padding: 18px;
  display: grid;
  gap: 16px;
}

.detailHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.detailEyebrow {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.detailTitle {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.detailStatusGroup {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detailTabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detailTab {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 14px;
  background: rgba(248, 250, 253, 0.96);
  color: var(--cdd-text-soft);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.detailTabActive {
  border-color: rgba(255, 107, 0, 0.22);
  background: linear-gradient(135deg, rgba(255, 107, 0, 0.1), rgba(255, 249, 244, 0.98));
  color: #9c4304;
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.08);
}

.overviewGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summaryCard,
.infoCard,
.deliveryBlock {
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 249, 255, 0.94));
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.05);
}

.summaryLabel,
.infoLabel,
.itemMetaLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summaryValue {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.4;
}

.infoGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.infoList {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.infoItem {
  display: grid;
  gap: 8px;
}

.infoValue {
  font-weight: 700;
  line-height: 1.7;
  word-break: break-word;
}

.sectionBlock {
  display: grid;
  gap: 12px;
}

.sectionTitle {
  font-size: 16px;
  font-weight: 800;
}

.listWrap {
  display: grid;
  gap: 10px;
}

.itemRow,
.logRow {
  display: grid;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.04);
}

.itemRow {
  grid-template-columns: minmax(0, 1.6fr) 0.6fr 0.8fr;
  align-items: center;
}

.logRow {
  grid-template-columns: minmax(0, 1.4fr) 0.7fr 0.9fr;
}

.itemMain,
.logMain {
  min-width: 0;
}

.itemName {
  font-size: 15px;
  font-weight: 800;
  line-height: 1.5;
}

.itemSpec {
  margin-top: 6px;
  color: var(--cdd-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.itemMeta {
  min-width: 0;
}

.itemMetaValue {
  margin-top: 8px;
  font-weight: 800;
  line-height: 1.6;
}

.logRemark {
  grid-column: 1 / -1;
  padding-top: 10px;
  color: var(--cdd-text-soft);
  border-top: 1px solid rgba(9, 29, 46, 0.06);
  line-height: 1.7;
}

.deliveryBlock {
  display: grid;
  gap: 12px;
  background:
    radial-gradient(circle at top right, rgba(255, 107, 0, 0.1), transparent 30%),
    linear-gradient(180deg, rgba(255, 250, 245, 0.98), rgba(255, 255, 255, 0.94));
}

.deliveryHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.deliveryHint {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  line-height: 1.7;
}

.fieldBlock {
  display: grid;
  gap: 8px;
}

.panelActions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.textarea {
  min-height: 96px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  font: inherit;
  resize: vertical;
}

@media (max-width: 1100px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbarActions {
    justify-content: flex-end;
  }

  .listSummaryCard {
    flex-direction: column;
  }

  .overviewGrid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .infoGrid {
    grid-template-columns: 1fr;
  }

  .formGrid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .tableHeader {
    display: none;
  }

  .row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 16px;
  }

  .cellLabel {
    display: block;
  }

  .itemRow,
  .logRow {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .toolbarActions,
  .detailStatusGroup,
  .detailTabs,
  .panelActions,
  .rowActions,
  .detailHead,
  .deliveryHead {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbarActions :global(button),
  .panelActions :global(button),
  .rowActions :global(button),
  .toolbarActions,
  .panelActions,
  .rowActions {
    width: 100%;
  }

  .toolbarActions :global(button),
  .panelActions :global(button),
  .rowActions :global(button) {
    width: 100%;
  }

  .row,
  .overviewGrid {
    grid-template-columns: 1fr;
  }

  .detailPanel {
    padding: 18px;
  }
}
</style>
