package com.cdd.product.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductCatalogStore {

    CategoryTemplateRecord createCategoryTemplate(String templateName,
                                                  String industryCode,
                                                  String templateVersion,
                                                  int maxLevel,
                                                  String templateDesc,
                                                  List<TemplateNodeDraft> nodeDrafts);

    Optional<CategoryTemplateRecord> findCategoryTemplate(long templateId);

    List<CategoryTemplateRecord> listCategoryTemplates();

    List<CategoryTemplateNodeRecord> listTemplateNodes(long templateId);

    boolean categoryTemplateExists(String templateName, String templateVersion);

    InitializeResult initializeCategoryTree(long merchantId, long storeId, long templateId);

    CategoryRecord createCategory(long merchantId,
                                  long storeId,
                                  long parentId,
                                  String categoryName,
                                  int sortOrder,
                                  boolean enabled,
                                  boolean visible,
                                  Long templateId,
                                  int categoryLevel);

    Optional<CategoryRecord> findCategory(long categoryId);

    List<CategoryRecord> listCategories(long merchantId, long storeId);

    boolean categoryNameExists(long merchantId, long storeId, long parentId, String categoryName);

    boolean categoryHasChildren(long merchantId, long storeId, long categoryId);

    boolean productExistsInCategory(long merchantId, long storeId, long categoryId);

    Optional<CategoryRecord> updateCategory(long categoryId,
                                            String categoryName,
                                            Integer sortOrder,
                                            Boolean enabled,
                                            Boolean visible);

    ProductRecord createProduct(long merchantId,
                                long storeId,
                                long categoryId,
                                String productName,
                                String productSubTitle,
                                List<SkuDraft> skuDrafts);

    Optional<ProductRecord> findProduct(long productId);

    List<ProductRecord> listProducts(long merchantId, long storeId, String status);

    Optional<ProductRecord> updateProduct(long productId,
                                          long categoryId,
                                          String productName,
                                          String productSubTitle,
                                          List<SkuDraft> skuDrafts);

    boolean skuCodeExists(long merchantId, String skuCode);

    Optional<ProductRecord> updateProductStatus(long productId, String status);

    List<SkuRecord> listSkusByProductId(long productId);

    Optional<SkuRecord> findSku(long skuId);

    Optional<StockRecord> findStock(long skuId);

    Optional<StockRecord> adjustStock(long skuId, int delta, String reason);

    ProductSalesRecord summarizePaidOrderSales(long merchantId, long storeId, long productId);

    record CategoryTemplateRecord(
            long id,
            String templateName,
            String industryCode,
            String templateVersion,
            int maxLevel,
            String status,
            String templateDesc,
            List<Long> nodeIds) {
    }

    record CategoryTemplateNodeRecord(
            long id,
            long templateId,
            String templateCategoryCode,
            String parentTemplateCategoryCode,
            String categoryName,
            int categoryLevel,
            int sortOrder,
            boolean enabled,
            boolean visible,
            String status) {
    }

    record TemplateNodeDraft(
            String templateCategoryCode,
            String parentTemplateCategoryCode,
            String categoryName,
            int categoryLevel,
            int sortOrder,
            boolean enabled,
            boolean visible) {
    }

    record InitializeResult(
            long templateId,
            int initializedCategoryCount) {
    }

    record CategoryRecord(
            long id,
            long merchantId,
            long storeId,
            Long templateId,
            long parentId,
            String categoryName,
            int categoryLevel,
            int sortOrder,
            boolean enabled,
            boolean visible) {
    }

    record ProductRecord(
            long id,
            long merchantId,
            long storeId,
            long categoryId,
            String productName,
            String productSubTitle,
            String status,
            List<Long> skuIds) {
    }

    record SkuRecord(
            long id,
            long productId,
            long merchantId,
            long storeId,
            String skuCode,
            String skuName,
            BigDecimal salePrice) {
    }

    record StockRecord(
            long skuId,
            long productId,
            long merchantId,
            long storeId,
            int availableStock,
            int lockedStock,
            String stockStatus,
            String lastReason) {
        public StockRecord(long skuId,
                           long productId,
                           long merchantId,
                           long storeId,
                           int availableStock,
                           int lockedStock,
                           String stockStatus) {
            this(skuId, productId, merchantId, storeId, availableStock, lockedStock, stockStatus, null);
        }
    }

    record SkuDraft(
            String skuCode,
            String skuName,
            BigDecimal salePrice,
            int availableStock) {
    }

    record ProductSalesRecord(
            int totalSalesQuantity,
            BigDecimal totalSalesAmount) {
    }
}
