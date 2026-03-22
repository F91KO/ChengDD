import { reactive } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { fetchOrderList } from '@/services/order';
import type { OrderSummaryResponseRaw } from '@/types/order';

type OrderCard = {
  orderNo: string;
  customer: string;
  items: string;
  amount: string;
  status: string;
  statusTone: 'primary' | 'info' | 'danger' | 'success';
  channel: string;
  time: string;
};

export const orderFilters = ['全部', '待支付', '待发货', '运输中', '已完成', '异常单'];

const mockOrders: OrderCard[] = [
  {
    orderNo: 'ORD-7782190',
    customer: '周辰',
    items: '2 件商品',
    amount: '¥1,240.00',
    status: '待发货',
    statusTone: 'primary',
    channel: '微信小程序',
    time: '2026-03-22 10:42',
  },
  {
    orderNo: 'ORD-9901234',
    customer: '罗安',
    items: '1 件商品',
    amount: '¥329.00',
    status: '运输中',
    statusTone: 'info',
    channel: 'H5 商城',
    time: '2026-03-22 09:16',
  },
  {
    orderNo: 'ORD-5541092',
    customer: '林清',
    items: '3 件商品',
    amount: '¥2,019.00',
    status: '异常单',
    statusTone: 'danger',
    channel: '抖音小店',
    time: '2026-03-21 18:05',
  },
  {
    orderNo: 'ORD-1120038',
    customer: '夏禾',
    items: '1 件商品',
    amount: '¥88.00',
    status: '已完成',
    statusTone: 'success',
    channel: '微信小程序',
    time: '2026-03-21 16:20',
  },
];

export const orders = reactive<OrderCard[]>([...mockOrders]);
export const orderLoadState = reactive({
  loading: false,
  source: 'mock' as 'mock' | 'remote',
  message: '当前展示演示数据。',
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
  if (payStatus.includes('paid') || status.includes('paid')) {
    return { text: '待发货', tone: 'primary' };
  }
  return { text: '待支付', tone: 'info' };
}

function mapRemoteOrder(item: OrderSummaryResponseRaw): OrderCard {
  const normalizedStatus = normalizeStatus(item);
  const amount = Number(item.paid_amount) > 0 ? item.paid_amount : item.payable_amount;
  return {
    orderNo: item.order_no,
    customer: `用户 ${item.user_id}`,
    items: '真实接口暂未返回商品明细',
    amount: normalizeAmount(amount),
    status: normalizedStatus.text,
    statusTone: normalizedStatus.tone,
    channel: '线上渠道',
    time: formatTime(item.created_at),
  };
}

function fallbackToMock(message: string) {
  replaceOrders(mockOrders);
  orderLoadState.source = 'mock';
  orderLoadState.message = message;
}

export async function loadOrders(force = false): Promise<void> {
  if (orderLoadState.loading) {
    return loadPromise ?? Promise.resolve();
  }
  if (orderLoadState.source === 'remote' && !force) {
    return;
  }

  loadPromise = (async () => {
    orderLoadState.loading = true;
    const authStore = useAuthStore();
    await authStore.ensureCurrentContext();

    const merchantId = authStore.merchantIdForQuery;
    const storeId = authStore.storeIdForQuery;
    if (!merchantId || !storeId) {
      fallbackToMock('当前账号上下文无法映射为数值型商户/店铺 ID，已使用演示数据。');
      orderLoadState.loading = false;
      return;
    }

    try {
      const remoteOrders = await fetchOrderList({
        merchantId,
        storeId,
        userId: authStore.userIdForQuery,
      });
      if (remoteOrders.length === 0) {
        fallbackToMock('真实接口返回空订单列表，已自动回退到演示数据。');
        orderLoadState.loading = false;
        return;
      }

      replaceOrders(remoteOrders.map(mapRemoteOrder));
      orderLoadState.source = 'remote';
      orderLoadState.message = '已使用真实订单接口数据。';
    } catch (error) {
      fallbackToMock(
        `订单接口调用失败，已回退演示数据：${error instanceof Error ? error.message : '未知错误'}`,
      );
    } finally {
      orderLoadState.loading = false;
    }
  })();

  await loadPromise;
}
