import { reactive } from 'vue';
import { fetchAllCategoryList, fetchProductList } from '@/services/product';
import { useAuthStore } from '@/stores/auth';
import type { ProductCategoryResponseRaw, ProductSummaryResponseRaw } from '@/types/product';

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

type LoadProductsArgs = {
  force?: boolean;
  status?: string;
  keyword?: string;
  page?: number;
  pageSize?: number;
};

export const products = reactive<ProductCard[]>([]);
export const productStats = reactive<ProductStat[]>([
  { label: '在售中', value: '0', tone: 'primary' },
  { label: '待发布', value: '0', tone: 'info' },
  { label: '已下架', value: '0', tone: 'danger' },
]);
export const productPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
});

export const productLoadState = reactive({
  loading: false,
  loaded: false,
  errorMessage: '',
  message: '等待加载真实商品数据。',
});

let loadPromise: Promise<void> | null = null;
let lastRequestKey = '';
let pendingArgs: LoadProductsArgs | null = null;

function replaceProductData(nextProducts: ProductCard[], nextStats: ProductStat[], page: number, pageSize: number, total: number) {
  products.splice(0, products.length, ...nextProducts);
  productStats.splice(0, productStats.length, ...nextStats);
  productPagination.page = page;
  productPagination.pageSize = pageSize;
  productPagination.total = total;
}

function buildRequestKey(status?: string, keyword?: string, page = 1, pageSize = 20): string {
  return `${status ?? ''}::${keyword?.trim().toLowerCase() ?? ''}::${page}::${pageSize}`;
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

function buildSalesLabel(item: ProductSummaryResponseRaw): string {
  const totalFromSummary = Number(item.sales_summary?.total_sales_quantity);
  if (Number.isFinite(totalFromSummary)) {
    return `${totalFromSummary} 件`;
  }
  return '0 件';
}

function mapRemoteProduct(
  item: ProductSummaryResponseRaw,
  categories: Map<number, ProductCategoryResponseRaw>,
): ProductCard {
  const mappedStatus = statusToCard(item.status);
  return {
    id: item.id,
    merchantId: item.merchant_id,
    storeId: item.store_id,
    categoryId: item.category_id,
    categoryName: categories.get(item.category_id)?.category_name ?? '分类缺失',
    name: item.product_name,
    sku: item.product_code || `SPU-${item.id}`,
    price: buildPriceLabel(item),
    sales: buildSalesLabel(item),
    inventory: buildInventoryLabel(item),
    status: mappedStatus.text,
    statusRaw: item.status,
    skuCount: item.sku_count,
    statusTone: mappedStatus.tone,
  };
}

function buildStats(onShelfTotal: number, draftTotal: number, offShelfTotal: number): ProductStat[] {
  return [
    { label: '在售中', value: String(onShelfTotal), tone: 'primary' },
    { label: '待发布', value: String(draftTotal), tone: 'info' },
    { label: '已下架', value: String(offShelfTotal), tone: 'danger' },
  ];
}

export async function loadProducts(force = false, status?: string, keyword?: string, page = productPagination.page, pageSize = productPagination.pageSize): Promise<void> {
  const normalizedKeyword = keyword?.trim() || '';
  const normalizedPage = Math.max(1, page);
  const normalizedPageSize = Math.max(1, pageSize);
  const requestKey = buildRequestKey(status, normalizedKeyword, normalizedPage, normalizedPageSize);

  if (productLoadState.loading) {
    pendingArgs = { force, status, keyword: normalizedKeyword, page: normalizedPage, pageSize: normalizedPageSize };
    await loadPromise;
    if (pendingArgs) {
      const nextArgs = pendingArgs;
      pendingArgs = null;
      return loadProducts(
        nextArgs.force ?? false,
        nextArgs.status,
        nextArgs.keyword,
        nextArgs.page ?? 1,
        nextArgs.pageSize ?? normalizedPageSize,
      );
    }
    return;
  }

  if (productLoadState.loaded && !force && lastRequestKey === requestKey) {
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
      replaceProductData([], buildStats(0, 0, 0), normalizedPage, normalizedPageSize, 0);
      productLoadState.loaded = true;
      productLoadState.message = '当前账号缺少真实商户上下文，无法加载商品数据。';
      productLoadState.errorMessage = productLoadState.message;
      productLoadState.loading = false;
      return;
    }

    try {
      const [initialPage, allCategories, onShelfPage, draftPage, offShelfPage] = await Promise.all([
        fetchProductList({
          merchantId,
          storeId,
          status,
          keyword: normalizedKeyword || undefined,
          page: normalizedPage,
          pageSize: normalizedPageSize,
        }),
        fetchAllCategoryList({ merchantId, storeId, pageSize: 200 }),
        fetchProductList({ merchantId, storeId, status: 'on_shelf', keyword: normalizedKeyword || undefined, page: 1, pageSize: 1 }),
        fetchProductList({ merchantId, storeId, status: 'draft', keyword: normalizedKeyword || undefined, page: 1, pageSize: 1 }),
        fetchProductList({ merchantId, storeId, status: 'off_shelf', keyword: normalizedKeyword || undefined, page: 1, pageSize: 1 }),
      ]);
      const fallbackPage = initialPage.total > 0 && initialPage.list.length === 0 && initialPage.page > 1
        ? Math.max(1, Math.ceil(initialPage.total / initialPage.page_size))
        : null;
      const remotePage = fallbackPage
        ? await fetchProductList({
          merchantId,
          storeId,
          status,
          keyword: normalizedKeyword || undefined,
          page: fallbackPage,
          pageSize: initialPage.page_size,
        })
        : initialPage;

      const categoryMap = new Map(allCategories.map((item) => [item.id, item]));

      replaceProductData(
        remotePage.list.map((item) => mapRemoteProduct(item, categoryMap)),
        buildStats(onShelfPage.total, draftPage.total, offShelfPage.total),
        remotePage.page,
        remotePage.page_size,
        remotePage.total,
      );
      productLoadState.loaded = true;
      lastRequestKey = requestKey;
      productLoadState.message = normalizedKeyword
        ? remotePage.total
          ? `已通过后端搜索“${normalizedKeyword}”，共返回 ${remotePage.total} 条商品。`
          : `后端搜索“${normalizedKeyword}”未返回商品结果。`
        : remotePage.total
          ? `已加载真实商品摘要、SKU 价格与库存信息，共 ${remotePage.total} 条。`
          : '当前商家暂无商品数据。';
    } catch (error) {
      replaceProductData([], buildStats(0, 0, 0), normalizedPage, normalizedPageSize, 0);
      productLoadState.loaded = true;
      productLoadState.errorMessage = error instanceof Error ? error.message : '商品接口调用失败。';
      productLoadState.message = productLoadState.errorMessage;
    } finally {
      productLoadState.loading = false;
    }
  })();

  await loadPromise;
}
