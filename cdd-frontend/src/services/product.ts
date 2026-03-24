import { requestApi } from '@/services/http';
import type {
  ProductCategoryResponseRaw,
  ProductCategoryTemplateResponseRaw,
  ProductDetailResponseRaw,
  InitializeCategoryTreeResponseRaw,
  ProductStockResponseRaw,
  ProductSummaryResponseRaw,
} from '@/types/product';

export async function fetchProductList(params: {
  merchantId: number;
  storeId: number;
  status?: string;
}): Promise<ProductSummaryResponseRaw[]> {
  return requestApi<ProductSummaryResponseRaw[]>({
    method: 'GET',
    url: '/product/spu',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      status: params.status,
    },
  });
}

export async function fetchProductDetail(productId: number): Promise<ProductDetailResponseRaw> {
  return requestApi<ProductDetailResponseRaw>({
    method: 'GET',
    url: `/product/spu/${productId}`,
  });
}

export async function createProduct(payload: {
  merchantId: number;
  storeId: number;
  categoryId: number;
  productName: string;
  productSubTitle?: string;
  skuCode: string;
  skuName: string;
  salePrice: number;
  availableStock: number;
}): Promise<ProductDetailResponseRaw> {
  return requestApi<ProductDetailResponseRaw>({
    method: 'POST',
    url: '/product/spu',
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      category_id: payload.categoryId,
      product_name: payload.productName,
      product_sub_title: payload.productSubTitle ?? '',
      skus: [
        {
          sku_code: payload.skuCode,
          sku_name: payload.skuName,
          sale_price: payload.salePrice,
          available_stock: payload.availableStock,
        },
      ],
    },
  });
}

export async function updateProduct(payload: {
  productId: number;
  merchantId: number;
  storeId: number;
  categoryId: number;
  productName: string;
  productSubTitle?: string;
  skus: Array<{
    skuCode: string;
    skuName: string;
    salePrice: number;
    availableStock: number;
  }>;
}): Promise<ProductDetailResponseRaw> {
  return requestApi<ProductDetailResponseRaw>({
    method: 'PUT',
    url: `/product/spu/${payload.productId}`,
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      category_id: payload.categoryId,
      product_name: payload.productName,
      product_sub_title: payload.productSubTitle ?? '',
      skus: payload.skus.map((sku) => ({
        sku_code: sku.skuCode,
        sku_name: sku.skuName,
        sale_price: sku.salePrice,
        available_stock: sku.availableStock,
      })),
    },
  });
}

export async function publishProduct(productId: number): Promise<ProductDetailResponseRaw> {
  return requestApi<ProductDetailResponseRaw>({
    method: 'POST',
    url: `/product/spu/${productId}/publish`,
  });
}

export async function unpublishProduct(productId: number): Promise<ProductDetailResponseRaw> {
  return requestApi<ProductDetailResponseRaw>({
    method: 'POST',
    url: `/product/spu/${productId}/unpublish`,
  });
}

export async function adjustProductStock(payload: {
  merchantId: number;
  storeId: number;
  productId: number;
  skuId: number;
  deltaStock: number;
  reason: string;
}): Promise<ProductStockResponseRaw> {
  return requestApi<ProductStockResponseRaw>({
    method: 'POST',
    url: '/product/stock/adjust',
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      product_id: payload.productId,
      sku_id: payload.skuId,
      delta_stock: payload.deltaStock,
      reason: payload.reason,
    },
  });
}

export async function fetchCategoryList(params: {
  merchantId: number;
  storeId: number;
}): Promise<ProductCategoryResponseRaw[]> {
  return requestApi<ProductCategoryResponseRaw[]>({
    method: 'GET',
    url: '/product/categories',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
    },
  });
}

export async function fetchCategoryTemplateList(): Promise<ProductCategoryTemplateResponseRaw[]> {
  return requestApi<ProductCategoryTemplateResponseRaw[]>({
    method: 'GET',
    url: '/product/category-templates',
  });
}

export async function initializeCategoryTree(payload: {
  merchantId: number;
  storeId: number;
  templateId: number;
}): Promise<InitializeCategoryTreeResponseRaw> {
  return requestApi<InitializeCategoryTreeResponseRaw>({
    method: 'POST',
    url: '/product/categories/init',
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      template_id: payload.templateId,
    },
  });
}

export async function createCategory(payload: {
  merchantId: number;
  storeId: number;
  parentId?: number;
  categoryName: string;
  sortOrder?: number;
  enabled?: boolean;
  visible?: boolean;
}): Promise<ProductCategoryResponseRaw> {
  return requestApi<ProductCategoryResponseRaw>({
    method: 'POST',
    url: '/product/categories',
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      parent_id: payload.parentId ?? 0,
      category_name: payload.categoryName,
      sort_order: payload.sortOrder ?? 0,
      is_enabled: payload.enabled ?? true,
      is_visible: payload.visible ?? true,
    },
  });
}

export async function updateCategory(payload: {
  categoryId: number;
  merchantId: number;
  storeId: number;
  categoryName?: string;
  sortOrder?: number;
  enabled?: boolean;
  visible?: boolean;
}): Promise<ProductCategoryResponseRaw> {
  return requestApi<ProductCategoryResponseRaw>({
    method: 'PUT',
    url: `/product/categories/${payload.categoryId}`,
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      category_name: payload.categoryName,
      sort_order: payload.sortOrder,
      is_enabled: payload.enabled,
      is_visible: payload.visible,
    },
  });
}

export async function createCategoryTemplate(payload: {
  templateName: string;
  industryCode: string;
  templateVersion: string;
  maxLevel: number;
  templateDesc?: string;
  categories: Array<{
    templateCategoryCode: string;
    parentTemplateCategoryCode?: string;
    categoryName: string;
    sortOrder?: number;
    enabled?: boolean;
    visible?: boolean;
  }>;
}): Promise<ProductCategoryTemplateResponseRaw> {
  return requestApi<ProductCategoryTemplateResponseRaw>({
    method: 'POST',
    url: '/product/category-templates',
    data: {
      template_name: payload.templateName,
      industry_code: payload.industryCode,
      template_version: payload.templateVersion,
      max_level: payload.maxLevel,
      template_desc: payload.templateDesc ?? '',
      categories: payload.categories.map((item) => ({
        template_category_code: item.templateCategoryCode,
        parent_template_category_code: item.parentTemplateCategoryCode ?? '',
        category_name: item.categoryName,
        sort_order: item.sortOrder ?? 0,
        is_enabled: item.enabled ?? true,
        is_visible: item.visible ?? true,
      })),
    },
  });
}
