import { reactive } from 'vue';
import { fetchCategoryList, fetchProductList } from '@/services/product';
import { fetchProductDailyList } from '@/services/report';
import { useAuthStore } from '@/stores/auth';
import type { ProductCategoryResponseRaw, ProductSummaryResponseRaw } from '@/types/product';
import type { ReportProductDailyResponseRaw } from '@/types/report';

export type ProductCard = {
  id: number;
  merchantId: number;
  storeId: number;
  categoryId: number;
  categoryName: string;
  name: string;
  sku: string;
  price: string;
  sales: string;
  inventory: string;
  status: string;
  statusRaw: string;
  skuCount: number;
  statusTone: 'primary' | 'default' | 'info';
};

export type ProductStat = {
  label: string;
  value: string;
  tone: 'primary' | 'info' | 'danger';
};

export const products = reactive<ProductCard[]>([]);
export const productStats = reactive<ProductStat[]>([
  { label: '在售中', value: '0', tone: 'primary' },
  { label: '待发布', value: '0', tone: 'info' },
  { label: '已下架', value: '0', tone: 'danger' },
]);

export const productLoadState = reactive({
  loading: false,
  loaded: false,
  errorMessage: '',
  message: '等待加载真实商品数据。',
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
  return { text: '待发布', tone: 'default' };
}

function formatCurrency(value: number | string): string {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return '¥0.00';
  }
  return `¥${parsed.toFixed(2)}`;
}

function buildPriceLabel(item: ProductSummaryResponseRaw): string {
  const minPrice = Number(item.price_summary?.min_sale_price);
  const maxPrice = Number(item.price_summary?.max_sale_price);
  if (!Number.isFinite(minPrice) || !Number.isFinite(maxPrice)) {
    return '未配置售价';
  }
  if (minPrice === maxPrice) {
    return formatCurrency(minPrice);
  }
  return `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`;
}

function buildInventoryLabel(item: ProductSummaryResponseRaw): string {
  if (!item.stock_summary) {
    return `${item.sku_count} 个 SKU`;
  }
  const available = item.stock_summary.total_available_stock;
  const locked = item.stock_summary.total_locked_stock;
  if (!Number.isFinite(available) || !Number.isFinite(locked)) {
    return `${item.sku_count} 个 SKU`;
  }
  return locked > 0 ? `${available} 可售 / ${locked} 锁定` : `${available} 可售`;
}

function buildSalesLabel(item: ProductSummaryResponseRaw, rows: ReportProductDailyResponseRaw[] | undefined): string {
  const totalFromSummary = Number(item.sales_summary?.total_sales_quantity);
  if (Number.isFinite(totalFromSummary) && totalFromSummary > 0) {
    return `${totalFromSummary} 件`;
  }
  if (!rows?.length) {
    return '近日报表暂无销量';
  }
  return `${rows.reduce((sum, row) => sum + Number(row.sale_count || 0), 0)} 件`;
}

function mapRemoteProduct(
  item: ProductSummaryResponseRaw,
  categories: Map<number, ProductCategoryResponseRaw>,
  reportRows: ReportProductDailyResponseRaw[] = [],
): ProductCard {
  const mappedStatus = statusToCard(item.status);
  return {
    id: item.id,
    merchantId: item.merchant_id,
    storeId: item.store_id,
    categoryId: item.category_id,
    categoryName: categories.get(item.category_id)?.category_name ?? `分类 ${item.category_id}`,
    name: item.product_name,
    sku: `SPU-${item.id}`,
    price: buildPriceLabel(item),
    sales: buildSalesLabel(item, reportRows),
    inventory: buildInventoryLabel(item),
    status: mappedStatus.text,
    statusRaw: item.status,
    skuCount: item.sku_count,
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
    { label: '已下架', value: String(offShelfCount), tone: 'danger' },
  ];
}

function buildReportMap(rows: ReportProductDailyResponseRaw[]): Map<number, ReportProductDailyResponseRaw[]> {
  const reportMap = new Map<number, ReportProductDailyResponseRaw[]>();
  rows.forEach((row) => {
    const current = reportMap.get(row.product_id) ?? [];
    current.push(row);
    reportMap.set(row.product_id, current);
  });
  return reportMap;
}

export async function loadProducts(force = false, status?: string): Promise<void> {
  if (productLoadState.loading) {
    return loadPromise ?? Promise.resolve();
  }
  if (productLoadState.loaded && !force && !status) {
    return;
  }

  loadPromise = (async () => {
    productLoadState.loading = true;
    productLoadState.errorMessage = '';
    const authStore = useAuthStore();
    await authStore.ensureCurrentContext();

    const merchantId = authStore.merchantIdForQuery;
    const storeId = authStore.storeIdForQuery;
    if (!merchantId || !storeId) {
      replaceProductData([], buildStatsFromRemote([]));
      productLoadState.loaded = true;
      productLoadState.message = '当前账号缺少真实商户上下文，无法加载商品数据。';
      productLoadState.errorMessage = productLoadState.message;
      productLoadState.loading = false;
      return;
    }

    try {
      const remoteProducts = await fetchProductList({
        merchantId,
        storeId,
        status,
      });

      const [categories, reportRows] = await Promise.all([
        fetchCategoryList({ merchantId, storeId }).catch(() => []),
        fetchProductDailyList({ merchantId, storeId }).catch(() => []),
      ]);

      const categoryMap = new Map(categories.map((item) => [item.id, item]));
      const reportMap = buildReportMap(reportRows);

      replaceProductData(
        remoteProducts.map((item) => mapRemoteProduct(item, categoryMap, reportMap.get(item.id))),
        buildStatsFromRemote(remoteProducts),
      );
      productLoadState.loaded = true;
      productLoadState.message = remoteProducts.length
        ? '已加载真实商品摘要、SKU 价格与库存信息。'
        : '当前商家暂无商品数据。';
    } catch (error) {
      replaceProductData([], buildStatsFromRemote([]));
      productLoadState.loaded = true;
      productLoadState.errorMessage = error instanceof Error ? error.message : '商品接口调用失败。';
      productLoadState.message = productLoadState.errorMessage;
    } finally {
      productLoadState.loading = false;
    }
  })();

  await loadPromise;
}
