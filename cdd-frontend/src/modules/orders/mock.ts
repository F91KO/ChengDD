import { reactive } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { fetchOrderList } from '@/services/order';
import type { OrderSummaryResponseRaw } from '@/types/order';

export type OrderCard = {
  orderNo: string;
  merchantId: number;
  storeId: number;
  userId: number;
  customer: string;
  items: string;
  amount: string;
  status: string;
  statusRaw: string;
  deliveryStatusRaw: string;
  statusTone: 'primary' | 'info' | 'danger' | 'success';
  channel: string;
  time: string;
};

export const orderFilters = ['全部', '待支付', '待发货', '运输中', '已完成', '异常单'];

export const orders = reactive<OrderCard[]>([]);
export const orderLoadState = reactive({
  loading: false,
  loaded: false,
  errorMessage: '',
  message: '等待加载真实订单数据。',
});

let loadPromise: Promise<void> | null = null;

function replaceOrders(nextOrders: OrderCard[]) {
  orders.splice(0, orders.length, ...nextOrders);
}

function normalizeAmount(value: number | string): string {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return '¥--';
  }
  return `¥${parsed.toFixed(2)}`;
}

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`;
}

function normalizeStatus(item: OrderSummaryResponseRaw): {
  text: OrderCard['status'];
  tone: OrderCard['statusTone'];
} {
  const status = item.order_status.toLowerCase();
  const payStatus = item.pay_status.toLowerCase();
  const deliveryStatus = item.delivery_status.toLowerCase();

  if (status.includes('cancel') || status.includes('refund') || status.includes('after')) {
    return { text: '异常单', tone: 'danger' };
  }
  if (status.includes('complete') || status.includes('finish')) {
    return { text: '已完成', tone: 'success' };
  }
  if (deliveryStatus.includes('deliver') || deliveryStatus.includes('ship')) {
    return { text: '运输中', tone: 'info' };
  }
  if (payStatus.includes('paid') || status.includes('paid') || status.includes('preparing')) {
    return { text: '待发货', tone: 'primary' };
  }
  return { text: '待支付', tone: 'info' };
}

function mapRemoteOrder(item: OrderSummaryResponseRaw): OrderCard {
  const normalizedStatus = normalizeStatus(item);
  const amount = Number(item.paid_amount) > 0 ? item.paid_amount : item.payable_amount;
  return {
    orderNo: item.order_no,
    merchantId: item.merchant_id,
    storeId: item.store_id,
    userId: item.user_id,
    customer: item.customer_identifier || `用户 ${item.user_id}`,
    items: item.product_summary || '暂无商品摘要',
    amount: normalizeAmount(amount),
    status: normalizedStatus.text,
    statusRaw: item.order_status,
    deliveryStatusRaw: item.delivery_status,
    statusTone: normalizedStatus.tone,
    channel: item.channel || '未知渠道',
    time: formatTime(item.created_at),
  };
}

export function filterToOrderStatus(filter: string): string | undefined {
  switch (filter) {
    case '待支付':
      return 'pending_pay';
    case '待发货':
      return 'paid';
    case '运输中':
      return 'shipped';
    case '已完成':
      return 'finished';
    case '异常单':
      return 'cancelled';
    default:
      return undefined;
  }
}

export async function loadOrders(force = false, orderStatus?: string): Promise<void> {
  if (orderLoadState.loading) {
    return loadPromise ?? Promise.resolve();
  }
  if (orderLoadState.loaded && !force && !orderStatus) {
    return;
  }

  loadPromise = (async () => {
    orderLoadState.loading = true;
    orderLoadState.errorMessage = '';
    const authStore = useAuthStore();
    await authStore.ensureCurrentContext();

    const merchantId = authStore.merchantIdForQuery;
    const storeId = authStore.storeIdForQuery;
    if (!merchantId || !storeId) {
      replaceOrders([]);
      orderLoadState.loaded = true;
      orderLoadState.message = '当前账号缺少真实商户上下文，无法加载订单数据。';
      orderLoadState.errorMessage = orderLoadState.message;
      orderLoadState.loading = false;
      return;
    }

    try {
      const remoteOrders = await fetchOrderList({
        merchantId,
        storeId,
        userId: authStore.userIdForQuery,
        orderStatus,
      });

      replaceOrders(remoteOrders.map(mapRemoteOrder));
      orderLoadState.loaded = true;
      orderLoadState.message = remoteOrders.length
        ? '已加载真实订单接口数据。'
        : '当前筛选条件下没有真实订单数据。';
    } catch (error) {
      replaceOrders([]);
      orderLoadState.loaded = true;
      orderLoadState.errorMessage = error instanceof Error ? error.message : '订单接口调用失败。';
      orderLoadState.message = orderLoadState.errorMessage;
    } finally {
      orderLoadState.loading = false;
    }
  })();

  await loadPromise;
}
