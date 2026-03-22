import { reactive } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { fetchProductList } from '@/services/product';
import type { ProductSummaryResponseRaw } from '@/types/product';

type ProductCard = {
  name: string;
  sku: string;
  price: string;
  sales: string;
  inventory: string;
  status: string;
  statusTone: 'primary' | 'default' | 'info';
};

type ProductStat = {
  label: string;
  value: string;
  tone: 'primary' | 'info' | 'danger';
};

const mockProducts: ProductCard[] = [
  {
    name: 'ChengDD Nexus 联名款高弹透气竞速跑鞋',
    sku: 'CDD-2024-NX01',
    price: '¥599.00',
    sales: '2,491',
    inventory: '842',
    status: '校验通过',
    statusTone: 'primary',
  },
  {
    name: '精钢系列 42mm 智能腕表',
    sku: 'CDD-WTCH-821',
    price: '¥1,280.00',
    sales: '104',
    inventory: '3 (预警)',
    status: '待完善',
    statusTone: 'default',
  },
  {
    name: 'HIFI 降噪无线头戴式耳机 Pro 版',
    sku: 'CDD-AU-092-B',
    price: '¥899.00',
    sales: '562',
    inventory: '45',
    status: '已下架',
    statusTone: 'info',
  },
];

const mockStats: ProductStat[] = [
  { label: '在售中', value: '128', tone: 'primary' },
  { label: '待发布', value: '12', tone: 'info' },
  { label: '库存预警', value: '5', tone: 'danger' },
];

export const products = reactive<ProductCard[]>([...mockProducts]);
export const productStats = reactive<ProductStat[]>([...mockStats]);

export const productLoadState = reactive({
  loading: false,
  source: 'mock' as 'mock' | 'remote',
  message: '当前展示演示数据。',
});

let loadPromise: Promise<void> | null = null;

function replaceProductData(nextProducts: ProductCard[], nextStats: ProductStat[]) {
  products.splice(0, products.length, ...nextProducts);
  productStats.splice(0, productStats.length, ...nextStats);
}

function statusToCard(status: string): { text: string; tone: ProductCard['statusTone'] } {
  const normalized = status.toLowerCase();
  if (normalized === 'on_shelf') {
    return { text: '在售中', tone: 'primary' };
  }
  if (normalized === 'off_shelf') {
    return { text: '已下架', tone: 'info' };
  }
  return { text: '待完善', tone: 'default' };
}

function mapRemoteProduct(item: ProductSummaryResponseRaw): ProductCard {
  const mappedStatus = statusToCard(item.status);
  return {
    name: item.product_name,
    sku: `SPU-${item.id}`,
    price: '¥--',
    sales: '--',
    inventory: `${item.sku_count} 个 SKU`,
    status: mappedStatus.text,
    statusTone: mappedStatus.tone,
  };
}

function buildStatsFromRemote(items: ProductSummaryResponseRaw[]): ProductStat[] {
  const onShelfCount = items.filter((item) => item.status.toLowerCase() === 'on_shelf').length;
  const draftCount = items.filter((item) => item.status.toLowerCase() === 'draft').length;
  const offShelfCount = items.filter((item) => item.status.toLowerCase() === 'off_shelf').length;
  return [
    { label: '在售中', value: String(onShelfCount), tone: 'primary' },
    { label: '待发布', value: String(draftCount), tone: 'info' },
    { label: '库存预警', value: String(offShelfCount), tone: 'danger' },
  ];
}

function fallbackToMock(message: string) {
  replaceProductData(mockProducts, mockStats);
  productLoadState.source = 'mock';
  productLoadState.message = message;
}

export async function loadProducts(force = false): Promise<void> {
  if (productLoadState.loading) {
    return loadPromise ?? Promise.resolve();
  }
  if (productLoadState.source === 'remote' && !force) {
    return;
  }

  loadPromise = (async () => {
    productLoadState.loading = true;
    const authStore = useAuthStore();
    await authStore.ensureCurrentContext();

    const merchantId = authStore.merchantIdForQuery;
    const storeId = authStore.storeIdForQuery;
    if (!merchantId || !storeId) {
      fallbackToMock('当前账号上下文无法映射为数值型商户/店铺 ID，已使用演示数据。');
      productLoadState.loading = false;
      return;
    }

    try {
      const remoteProducts = await fetchProductList({
        merchantId,
        storeId,
      });

      if (remoteProducts.length === 0) {
        fallbackToMock('真实接口返回空商品列表，已自动回退到演示数据。');
        productLoadState.loading = false;
        return;
      }

      replaceProductData(
        remoteProducts.map(mapRemoteProduct),
        buildStatsFromRemote(remoteProducts),
      );
      productLoadState.source = 'remote';
      productLoadState.message = '已使用真实商品接口数据。';
    } catch (error) {
      fallbackToMock(
        `商品接口调用失败，已回退演示数据：${error instanceof Error ? error.message : '未知错误'}`,
      );
    } finally {
      productLoadState.loading = false;
    }
  })();

  await loadPromise;
}
