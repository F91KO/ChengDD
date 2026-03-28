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

export const orderFilters = ['全部', '待支付', '待发货', '待收货', '已完成', '已取消'];

export const orders = reactive<OrderCard[]>([]);
export const orderPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
});
export const orderLoadState = reactive({
  loading: false,
  loaded: false,
  errorMessage: '',
  message: '等待加载真实订单数据。',
});

let loadPromise: Promise<void> | null = null;
let lastRequestKey = '';
let pendingArgs: { force?: boolean; orderStatus?: string; page?: number; pageSize?: number } | null = null;

function replaceOrders(nextOrders: OrderCard[], page: number, pageSize: number, total: number) {
  orders.splice(0, orders.length, ...nextOrders);
  orderPagination.page = page;
  orderPagination.pageSize = pageSize;
  orderPagination.total = total;
}

function normalizeAmount(value: number | string): string {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return '¥-';
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

function normalizeChannel(channel: string): string {
  if (!channel) {
    return '未知渠道';
  }
  const normalized = channel.toLowerCase();
  if (normalized.includes('mini')) {
    return '小程序';
  }
  if (normalized.includes('app')) {
    return 'App';
  }
  if (normalized.includes('web') || normalized.includes('h5')) {
    return 'H5';
  }
  return channel;
}

function normalizeCustomer(value: string, userId: number): string {
  if (!value || value.toLowerCase() === 'unknown') {
    return `用户 ${userId}`;
  }
  return value;
}

function normalizeStatus(item: OrderSummaryResponseRaw): {
  text: OrderCard['status'];
  tone: OrderCard['statusTone'];
} {
  const status = item.order_status.toLowerCase();
  if (status.includes('cancel')) {
    return { text: '已取消', tone: 'danger' };
  }
  if (status.includes('complete') || status.includes('finished')) {
    return { text: '已完成', tone: 'success' };
  }
  if (status.includes('pending_receive') || status.includes('shipped')) {
    return { text: '待收货', tone: 'info' };
  }
  if (status.includes('pending_ship') || status.includes('paid') || status.includes('preparing')) {
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
    customer: normalizeCustomer(item.customer_identifier, item.user_id),
    items: item.product_summary || '暂无商品摘要',
    amount: normalizeAmount(amount),
    status: normalizedStatus.text,
    statusRaw: item.order_status,
    deliveryStatusRaw: item.delivery_status,
    statusTone: normalizedStatus.tone,
    channel: normalizeChannel(item.channel),
    time: formatTime(item.created_at),
  };
}

export function filterToOrderStatus(filter: string): string | undefined {
  switch (filter) {
    case '待支付':
      return 'pending_pay';
    case '待发货':
      return 'pending_ship';
    case '待收货':
      return 'pending_receive';
    case '已完成':
      return 'completed';
    case '已取消':
      return 'cancelled';
    default:
      return undefined;
  }
}

function buildRequestKey(orderStatus?: string, page = 1, pageSize = 20): string {
  return `${orderStatus ?? ''}::${page}::${pageSize}`;
}

export async function loadOrders(force = false, orderStatus?: string, page = orderPagination.page, pageSize = orderPagination.pageSize): Promise<void> {
  const normalizedPage = Math.max(1, page);
  const normalizedPageSize = Math.max(1, pageSize);
  const requestKey = buildRequestKey(orderStatus, normalizedPage, normalizedPageSize);

  if (orderLoadState.loading) {
    pendingArgs = { force, orderStatus, page: normalizedPage, pageSize: normalizedPageSize };
    await loadPromise;
    if (pendingArgs) {
      const nextArgs = pendingArgs;
      pendingArgs = null;
      return loadOrders(
        nextArgs.force ?? false,
        nextArgs.orderStatus,
        nextArgs.page ?? normalizedPage,
        nextArgs.pageSize ?? normalizedPageSize,
      );
    }
    return;
  }
  if (orderLoadState.loaded && !force && lastRequestKey === requestKey) {
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
      replaceOrders([], normalizedPage, normalizedPageSize, 0);
      orderLoadState.loaded = true;
      orderLoadState.message = '当前账号缺少真实商户上下文，无法加载订单数据。';
      orderLoadState.errorMessage = orderLoadState.message;
      orderLoadState.loading = false;
      return;
    }

    try {
      const initialPage = await fetchOrderList({
        merchantId,
        storeId,
        userId: authStore.userIdForQuery,
        orderStatus,
        page: normalizedPage,
        pageSize: normalizedPageSize,
      });
      const fallbackPage = initialPage.total > 0 && initialPage.list.length === 0 && initialPage.page > 1
        ? Math.max(1, Math.ceil(initialPage.total / initialPage.page_size))
        : null;
      const remotePage = fallbackPage
        ? await fetchOrderList({
          merchantId,
          storeId,
          userId: authStore.userIdForQuery,
          orderStatus,
          page: fallbackPage,
          pageSize: initialPage.page_size,
        })
        : initialPage;

      replaceOrders(remotePage.list.map(mapRemoteOrder), remotePage.page, remotePage.page_size, remotePage.total);
      orderLoadState.loaded = true;
      lastRequestKey = requestKey;
      orderLoadState.message = remotePage.total
        ? `已加载真实订单接口数据，共 ${remotePage.total} 条。`
        : '当前筛选条件下没有真实订单数据。';
    } catch (error) {
      replaceOrders([], normalizedPage, normalizedPageSize, 0);
      orderLoadState.loaded = true;
      orderLoadState.errorMessage = error instanceof Error ? error.message : '订单接口调用失败。';
      orderLoadState.message = orderLoadState.errorMessage;
    } finally {
      orderLoadState.loading = false;
    }
  })();

  await loadPromise;
}
