export interface ProductSummaryResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  category_id: number;
  product_name: string;
  status: string;
  sku_count: number;
}

export interface ProductCategoryResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  template_id: number | null;
  parent_id: number;
  category_name: string;
  category_level: number;
  sort_order: number;
  is_enabled: boolean;
  is_visible: boolean;
}

export interface ProductSkuResponseRaw {
  id: number;
  product_id: number;
  sku_code: string;
  sku_name: string;
  sale_price: number | string;
  available_stock: number;
  locked_stock: number;
  stock_status: string;
}

export interface ProductStockResponseRaw {
  product_id: number;
  sku_id: number;
  available_stock: number;
  locked_stock: number;
  stock_status: string;
}

export interface ProductDetailResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  category_id: number;
  product_name: string;
  product_sub_title: string | null;
  status: string;
  skus: ProductSkuResponseRaw[];
}
