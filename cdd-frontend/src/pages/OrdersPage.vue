<template>
  <WorkspaceLayout
    eyebrow="Orders"
    title="订单管理"
    description="先提供筛选、状态总览和重点订单列表，后续再把详情抽屉、发货与退款动作接进来。"
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
        <UiButton variant="secondary">批量导出</UiButton>
        <UiButton>创建发货单</UiButton>
      </div>
    </section>

    <UiStatePanel
      :tone="orderLoadState.loading ? 'loading' : orderLoadState.source === 'remote' ? 'info' : 'empty'"
      :title="
        orderLoadState.loading
          ? '正在加载订单列表'
          : orderLoadState.source === 'remote'
            ? '当前使用真实订单接口'
            : '当前展示演示订单数据'
      "
      :description="
        orderLoadState.loading
          ? '正在优先请求真实订单接口，若服务不可用会自动回退到演示数据。'
          : orderLoadState.message
      "
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
        </div>
        <article
          v-for="order in filteredOrders"
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
        </article>
        <UiStatePanel
          v-if="!orderLoadState.loading && filteredOrders.length === 0"
          tone="empty"
          title="当前筛选下没有订单"
          description="可以切回“全部”查看完整订单列表，或等待真实订单数据同步后再继续跟单。"
        >
          <UiButton variant="secondary" @click="activeFilter = '全部'">查看全部订单</UiButton>
        </UiStatePanel>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { loadOrders, orderFilters, orderLoadState, orders } from '@/modules/orders/mock';

const activeFilter = ref('全部');
const statusMap: Record<string, string> = {
  待支付: '待支付',
  待发货: '待发货',
  运输中: '运输中',
  已完成: '已完成',
  异常单: '异常单',
};
const filteredOrders = computed(() => {
  if (activeFilter.value === '全部') {
    return orders;
  }

  return orders.filter((order) => order.status === statusMap[activeFilter.value]);
});

onMounted(() => {
  void loadOrders();
});

watch(activeFilter, () => {
  if (activeFilter.value === '全部') {
    void loadOrders();
  }
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

.tableCard {
  display: grid;
  gap: 0;
  overflow: hidden;
}

.tableHeader,
.row {
  display: grid;
  grid-template-columns: 1.4fr 0.7fr 1fr 0.8fr 0.8fr 1fr;
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

  .row {
    grid-template-columns: 1fr;
  }
}
</style>
