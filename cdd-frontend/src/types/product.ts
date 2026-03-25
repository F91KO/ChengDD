export interface ProductSummaryResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  category_id: number;
  product_code: string;
  product_name: string;
  product_sub_title: string | null;
  status: string;
  sku_count: number;
  price_summary: {
    min_sale_price: number | string;
    max_sale_price: number | string;
  };
  sales_summary: {
    total_sales_quantity: number;
    total_sales_amount: number | string;
  };
  stock_summary: {
    total_available_stock: number;
    total_locked_stock: number;
    stock_status: string;
  };
  sku_summaries: ProductSkuResponseRaw[];
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

export interface ProductCategoryTemplateNodeResponseRaw {
  id: number;
  template_category_code: string;
  parent_template_category_code: string | null;
  category_name: string;
  category_level: number;
  sort_order: number;
  is_enabled: boolean;
  is_visible: boolean;
}

export interface ProductCategoryTemplateResponseRaw {
  id: number;
  template_name: string;
  industry_code: string;
  template_version: string;
  max_level: number;
  status: string;
  template_desc: string | null;
  categories: ProductCategoryTemplateNodeResponseRaw[];
}

export interface InitializeCategoryTreeResponseRaw {
  merchant_id: number;
  store_id: number;
  template_id: number;
  initialized_category_count: number;
  message: string;
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
  product_code: string;
  product_name: string;
  product_sub_title: string | null;
  status: string;
  skus: ProductSkuResponseRaw[];
}
