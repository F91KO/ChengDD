import { requestApi } from '@/services/http';
import type {
  ProductCategoryResponseRaw,
  ProductDetailResponseRaw,
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
