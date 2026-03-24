<template>
  <WorkspaceLayout
    eyebrow="Orders"
    title="订单管理"
    description="当前页面已接入真实订单列表、详情、履约推进、状态日志和导出能力。"
  >
    <section :class="$style.toolbar">
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
      <div :class="$style.toolbarActions">
        <UiButton variant="secondary" @click="handleExportOrders">批量导出</UiButton>
        <UiButton @click="openDeliveryPanelFromList">推进发货</UiButton>
      </div>
    </section>

    <UiStatePanel
      :tone="orderLoadState.loading ? 'loading' : orderLoadState.errorMessage ? 'error' : 'info'"
      :title="
        orderLoadState.loading
          ? '正在加载订单列表'
          : orderLoadState.errorMessage
            ? '订单接口调用失败'
            : '当前使用真实订单接口'
      "
      :description="
        orderLoadState.loading
          ? '正在加载真实订单接口数据。'
          : orderLoadState.message
      "
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.tableWrap">
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
          <div>
            <div :class="$style.mainText">{{ order.orderNo }}</div>
            <div :class="$style.subText">{{ order.items }}</div>
          </div>
          <div :class="$style.mainText">{{ order.customer }}</div>
          <div :class="$style.subText">{{ order.channel }}</div>
          <div :class="$style.mainText">{{ order.amount }}</div>
          <div>
            <UiTag :tone="order.statusTone as 'primary' | 'info' | 'danger' | 'success'">
              {{ order.status }}
            </UiTag>
          </div>
          <div :class="$style.subText">{{ order.time }}</div>
          <div :class="$style.rowActions">
            <UiButton variant="secondary" @click="handleViewDetail(order)">查看详情</UiButton>
            <UiButton
              v-if="order.status === '待发货' || order.status === '运输中'"
              variant="secondary"
              @click="openDeliveryPanel(order)"
            >
              推进履约
            </UiButton>
          </div>
        </article>
        <UiStatePanel
          v-if="!orderLoadState.loading && orders.length === 0"
          tone="empty"
          title="当前筛选下没有订单"
          description="当前真实接口返回空列表，可以切换筛选条件后重新查询。"
        >
          <UiButton variant="secondary" @click="activeFilter = '全部'">查看全部订单</UiButton>
        </UiStatePanel>
      </UiCard>
    </section>

    <UiCard v-if="selectedOrderDetail" elevated :class="$style.detailPanel">
      <div :class="$style.detailHead">
        <div>
          <div :class="$style.mainText">订单详情</div>
          <div :class="$style.subText">{{ selectedOrderDetail.order_no }}</div>
        </div>
        <UiButton variant="secondary" @click="closeDetailPanel">关闭面板</UiButton>
      </div>

      <section :class="$style.detailGrid">
        <div>
          <div :class="$style.subText">订单状态</div>
          <div :class="$style.mainText">{{ selectedOrderDetail.order_status }}</div>
        </div>
        <div>
          <div :class="$style.subText">履约状态</div>
          <div :class="$style.mainText">{{ selectedOrderDetail.delivery_status }}</div>
        </div>
        <div>
          <div :class="$style.subText">收货人</div>
          <div :class="$style.mainText">{{ selectedOrderDetail.receiver_name || '-' }}</div>
        </div>
        <div>
          <div :class="$style.subText">收货地址</div>
          <div :class="$style.mainText">{{ selectedOrderDetail.receiver_address || '-' }}</div>
        </div>
      </section>

      <section :class="$style.detailBlock">
        <div :class="$style.mainText">商品明细</div>
        <article v-for="item in selectedOrderDetail.items" :key="item.id" :class="$style.detailRow">
          <div>{{ item.product_name }} / {{ item.sku_name }}</div>
          <div>{{ item.quantity }} 件</div>
          <div>{{ item.line_amount }}</div>
        </article>
      </section>

      <section :class="$style.detailBlock">
        <div :class="$style.mainText">状态日志</div>
        <article
          v-for="log in selectedOrderDetail.status_logs"
          :key="`${log.operate_type}-${log.created_at}`"
          :class="$style.detailRow"
        >
          <div>{{ log.operate_type }}</div>
          <div>{{ log.from_status }} -> {{ log.to_status }}</div>
          <div>{{ log.created_at }}</div>
        </article>
      </section>

      <section v-if="deliveryTarget" :class="$style.deliveryBlock">
        <div :class="$style.mainText">履约操作</div>
        <div :class="$style.subText">
          将订单推进到 {{ deliveryTarget.status === '待发货' ? 'shipped' : 'received' }}
        </div>
        <textarea
          v-model="deliveryRemark"
          :class="$style.textarea"
          placeholder="请输入履约备注"
        />
        <div :class="$style.toolbarActions">
          <UiButton variant="secondary" @click="deliveryRemark = ''">清空备注</UiButton>
          <UiButton :disabled="submitting" @click="submitDelivery">
            {{ submitting ? '正在提交...' : '提交履约' }}
          </UiButton>
        </div>
      </section>
    </UiCard>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { exportOrdersCsv, fetchOrderDetail, updateOrderDelivery } from '@/services/order';
import {
  filterToOrderStatus,
  loadOrders,
  orderFilters,
  orderLoadState,
  orders,
  type OrderCard,
} from '@/modules/orders/mock';
import type { OrderDetailResponseRaw } from '@/types/order';

const activeFilter = ref('全部');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const selectedOrder = ref<OrderCard | null>(null);
const selectedOrderDetail = ref<OrderDetailResponseRaw | null>(null);
const deliveryTarget = ref<OrderCard | null>(null);
const deliveryRemark = ref('');
const submitting = ref(false);

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

async function refreshOrders() {
  await loadOrders(true, filterToOrderStatus(activeFilter.value));
}

async function handleViewDetail(order: OrderCard) {
  try {
    selectedOrder.value = order;
    selectedOrderDetail.value = await fetchOrderDetail({
      orderNo: order.orderNo,
      merchantId: order.merchantId,
      storeId: order.storeId,
      userId: order.userId,
    });
    deliveryTarget.value = null;
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '查询订单详情失败。', 'error');
  }
}

async function openDeliveryPanel(order: OrderCard) {
  try {
    await handleViewDetail(order);
    deliveryTarget.value = order;
    deliveryRemark.value = '前端联调推进履约';
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '履约推进失败。', 'error');
  }
}

async function submitDelivery() {
  if (!deliveryTarget.value) {
    return;
  }
  try {
    submitting.value = true;
    const nextStatus = deliveryTarget.value.status === '待发货' ? 'shipped' : 'received';
    await updateOrderDelivery({
      orderNo: deliveryTarget.value.orderNo,
      merchantId: deliveryTarget.value.merchantId,
      storeId: deliveryTarget.value.storeId,
      userId: deliveryTarget.value.userId,
      deliveryStatus: nextStatus,
      remark: deliveryRemark.value.trim(),
    });
    await refreshOrders();
    await handleViewDetail(deliveryTarget.value);
    setActionMessage(`订单 ${deliveryTarget.value.orderNo} 已推进到 ${nextStatus}。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '履约推进失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

function openDeliveryPanelFromList() {
  const target = orders.find((item) => item.status === '待发货' || item.status === '运输中');
  if (!target) {
    setActionMessage('当前列表没有可推进履约的订单。', 'error');
    return;
  }
  void openDeliveryPanel(target);
}

async function handleExportOrders() {
  try {
    const firstOrder = orders[0];
    if (!firstOrder) {
      throw new Error('当前没有可导出的订单。');
    }
    const blob = await exportOrdersCsv({
      merchantId: firstOrder.merchantId,
      storeId: firstOrder.storeId,
      userId: firstOrder.userId,
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
  selectedOrder.value = null;
  selectedOrderDetail.value = null;
  deliveryTarget.value = null;
  deliveryRemark.value = '';
}

onMounted(() => {
  void refreshOrders();
});

watch(activeFilter, () => {
  void refreshOrders();
});
</script>

<style module>
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filterChip {
  min-height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  color: var(--cdd-text-soft);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
  font-size: 13px;
  font-weight: 800;
}

.filterChipActive {
  color: #fff;
  background: linear-gradient(135deg, var(--cdd-primary-deep), var(--cdd-primary));
}

.toolbarActions {
  display: flex;
  gap: 12px;
}

.tableWrap {
  min-width: 0;
}

.detailPanel {
  margin-top: 18px;
  display: grid;
  gap: 18px;
}

.detailHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.detailBlock,
.deliveryBlock {
  display: grid;
  gap: 12px;
}

.detailRow {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 1fr;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
}

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

.tableCard {
  display: grid;
  gap: 0;
  overflow: hidden;
}

.tableHeader,
.row {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr 1fr 0.8fr 0.8fr 1fr 1.2fr;
  gap: 16px;
  align-items: center;
  padding: 18px 22px;
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

.mainText {
  font-size: 14px;
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

@media (max-width: 1100px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbarActions {
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .tableHeader {
    display: none;
  }

  .row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .toolbarActions {
    flex-direction: column;
  }

  .row,
  .detailGrid,
  .detailRow {
    grid-template-columns: 1fr;
  }

  .detailHead {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
