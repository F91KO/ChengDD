package com.cdd.product.infrastructure.memory;

import com.cdd.common.core.page.PageQuery;
import com.cdd.common.core.page.PageResult;
import com.cdd.product.infrastructure.ProductCatalogStore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.util.StringUtils;

public class InMemoryProductCatalogStore implements ProductCatalogStore {

    private final AtomicLong templateIdGenerator = new AtomicLong(2_000_000L);
    private final AtomicLong templateNodeIdGenerator = new AtomicLong(2_050_000L);
    private final AtomicLong categoryIdGenerator = new AtomicLong(2_100_000L);
    private final AtomicLong productIdGenerator = new AtomicLong(3_100_000L);
    private final AtomicLong skuIdGenerator = new AtomicLong(3_200_000L);

    private final Map<Long, CategoryTemplateRecord> categoryTemplates = new ConcurrentHashMap<>();
    private final Map<Long, CategoryTemplateNodeRecord> categoryTemplateNodes = new ConcurrentHashMap<>();
    private final Map<Long, CategoryRecord> categories = new ConcurrentHashMap<>();
    private final Map<Long, ProductRecord> products = new ConcurrentHashMap<>();
    private final Map<Long, SkuRecord> skus = new ConcurrentHashMap<>();
    private final Map<Long, StockRecord> stocks = new ConcurrentHashMap<>();
    private final Map<String, Long> merchantSkuCodeIndex = new ConcurrentHashMap<>();

    public InMemoryProductCatalogStore() {
        seedDefaultTemplate();
        seedDefaultMerchantCatalog();
    }

    @Override
    public synchronized CategoryTemplateRecord createCategoryTemplate(String templateName,
                                                                      String industryCode,
                                                                      String templateVersion,
                                                                      int maxLevel,
                                                                      String templateDesc,
                                                                      List<TemplateNodeDraft> nodeDrafts) {
        long templateId = templateIdGenerator.incrementAndGet();
        List<Long> nodeIds = new ArrayList<>(nodeDrafts.size());
        for (TemplateNodeDraft draft : nodeDrafts) {
            long nodeId = templateNodeIdGenerator.incrementAndGet();
            CategoryTemplateNodeRecord node = new CategoryTemplateNodeRecord(
                    nodeId,
                    templateId,
                    draft.templateCategoryCode(),
                    draft.parentTemplateCategoryCode(),
                    draft.categoryName(),
                    draft.categoryLevel(),
                    draft.sortOrder(),
                    draft.enabled(),
                    draft.visible(),
                    "enabled");
            categoryTemplateNodes.put(nodeId, node);
            nodeIds.add(nodeId);
        }
        CategoryTemplateRecord created = new CategoryTemplateRecord(
                templateId,
                templateName,
                industryCode,
                templateVersion,
                maxLevel,
                "enabled",
                templateDesc,
                List.copyOf(nodeIds));
        categoryTemplates.put(templateId, created);
        return created;
    }

    @Override
    public Optional<CategoryTemplateRecord> findCategoryTemplate(long templateId) {
        return Optional.ofNullable(categoryTemplates.get(templateId));
    }

    @Override
    public List<CategoryTemplateRecord> listCategoryTemplates() {
        return categoryTemplates.values().stream()
                .sorted(Comparator.comparingLong(CategoryTemplateRecord::id))
                .toList();
    }

    @Override
    public List<CategoryTemplateNodeRecord> listTemplateNodes(long templateId) {
        CategoryTemplateRecord template = categoryTemplates.get(templateId);
        if (template == null) {
            return List.of();
        }
        List<CategoryTemplateNodeRecord> result = new ArrayList<>(template.nodeIds().size());
        for (Long nodeId : template.nodeIds()) {
            CategoryTemplateNodeRecord node = categoryTemplateNodes.get(nodeId);
            if (node != null) {
                result.add(node);
            }
        }
        result.sort(Comparator.comparingInt(CategoryTemplateNodeRecord::categoryLevel)
                .thenComparingInt(CategoryTemplateNodeRecord::sortOrder)
                .thenComparingLong(CategoryTemplateNodeRecord::id));
        return result;
    }

    @Override
    public boolean categoryTemplateExists(String templateName, String templateVersion) {
        return categoryTemplates.values().stream()
                .anyMatch(template -> template.templateName().equals(templateName)
                        && template.templateVersion().equals(templateVersion));
    }

    @Override
    public synchronized InitializeResult initializeCategoryTree(long merchantId, long storeId, long templateId) {
        CategoryTemplateRecord template = categoryTemplates.get(templateId);
        if (template == null) {
            return new InitializeResult(templateId, 0);
        }
        List<CategoryTemplateNodeRecord> nodes = listTemplateNodes(templateId);
        Map<String, Long> categoryIdMapping = new HashMap<>();
        int initializedCount = 0;
        for (CategoryTemplateNodeRecord node : nodes) {
            long parentId = 0L;
            if (StringUtils.hasText(node.parentTemplateCategoryCode())) {
                Long resolvedParentId = categoryIdMapping.get(node.parentTemplateCategoryCode());
                if (resolvedParentId == null) {
                    throw new IllegalStateException("模板父节点未找到: " + node.parentTemplateCategoryCode());
                }
                parentId = resolvedParentId;
            }
            Optional<CategoryRecord> existing = findCategoryByParentAndName(merchantId, storeId, parentId, node.categoryName());
            if (existing.isPresent()) {
                categoryIdMapping.put(node.templateCategoryCode(), existing.get().id());
                continue;
            }
            CategoryRecord created = createCategory(
                    merchantId,
                    storeId,
                    parentId,
                    node.categoryName(),
                    node.sortOrder(),
                    node.enabled(),
                    node.visible(),
                    templateId,
                    node.categoryLevel());
            categoryIdMapping.put(node.templateCategoryCode(), created.id());
            initializedCount += 1;
        }
        return new InitializeResult(templateId, initializedCount);
    }

    @Override
    public synchronized CategoryRecord createCategory(long merchantId,
                                                      long storeId,
                                                      long parentId,
                                                      String categoryName,
                                                      int sortOrder,
                                                      boolean enabled,
                                                      boolean visible,
                                                      Long templateId,
                                                      int categoryLevel) {
        long id = categoryIdGenerator.incrementAndGet();
        CategoryRecord record = new CategoryRecord(
                id,
                merchantId,
                storeId,
                templateId,
                parentId,
                categoryName,
                categoryLevel,
                sortOrder,
                enabled,
                visible);
        categories.put(id, record);
        return record;
    }

    @Override
    public Optional<CategoryRecord> findCategory(long categoryId) {
        return Optional.ofNullable(categories.get(categoryId));
    }

    @Override
    public List<CategoryRecord> listCategories(long merchantId, long storeId, String keyword) {
        return categories.values().stream()
                .filter(category -> category.merchantId() == merchantId && category.storeId() == storeId)
                .filter(category -> keyword == null
                        || keyword.isBlank()
                        || category.categoryName().toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparingInt(CategoryRecord::categoryLevel)
                        .thenComparingInt(CategoryRecord::sortOrder)
                        .thenComparingLong(CategoryRecord::id))
                .toList();
    }

    @Override
    public boolean categoryNameExists(long merchantId, long storeId, long parentId, String categoryName) {
        return categories.values().stream()
                .anyMatch(category -> category.merchantId() == merchantId
                        && category.storeId() == storeId
                        && category.parentId() == parentId
                        && category.categoryName().equals(categoryName));
    }

    @Override
    public boolean categoryHasChildren(long merchantId, long storeId, long categoryId) {
        return categories.values().stream()
                .anyMatch(category -> category.merchantId() == merchantId
                        && category.storeId() == storeId
                        && category.parentId() == categoryId);
    }

    @Override
    public boolean productExistsInCategory(long merchantId, long storeId, long categoryId) {
        return products.values().stream()
                .anyMatch(product -> product.merchantId() == merchantId
                        && product.storeId() == storeId
                        && product.categoryId() == categoryId);
    }

    @Override
    public synchronized Optional<CategoryRecord> updateCategory(long categoryId,
                                                                String categoryName,
                                                                Integer sortOrder,
                                                                Boolean enabled,
                                                                Boolean visible) {
        CategoryRecord current = categories.get(categoryId);
        if (current == null) {
            return Optional.empty();
        }
        CategoryRecord updated = new CategoryRecord(
                current.id(),
                current.merchantId(),
                current.storeId(),
                current.templateId(),
                current.parentId(),
                categoryName == null ? current.categoryName() : categoryName,
                current.categoryLevel(),
                sortOrder == null ? current.sortOrder() : sortOrder,
                enabled == null ? current.enabled() : enabled,
                visible == null ? current.visible() : visible);
        categories.put(categoryId, updated);
        return Optional.of(updated);
    }

    @Override
    public synchronized ProductRecord createProduct(long merchantId,
                                                    long storeId,
                                                    long categoryId,
                                                    String productName,
                                                    String productSubTitle,
                                                    List<SkuDraft> skuDrafts) {
        long productId = productIdGenerator.incrementAndGet();
        String productCode = buildProductCode(productId);
        List<Long> productSkuIds = new ArrayList<>(skuDrafts.size());
        for (SkuDraft draft : skuDrafts) {
            long skuId = skuIdGenerator.incrementAndGet();
            SkuRecord sku = new SkuRecord(
                    skuId,
                    productId,
                    merchantId,
                    storeId,
                    draft.skuCode(),
                    draft.skuName(),
                    draft.salePrice());
            skus.put(skuId, sku);
            productSkuIds.add(skuId);
            stocks.put(skuId, new StockRecord(skuId, productId, merchantId, storeId, draft.availableStock(), 0, toStockStatus(draft.availableStock())));
            merchantSkuCodeIndex.put(merchantSkuCodeKey(merchantId, draft.skuCode()), skuId);
        }
        ProductRecord product = new ProductRecord(
                productId,
                merchantId,
                storeId,
                categoryId,
                productCode,
                productName,
                productSubTitle,
                "draft",
                List.copyOf(productSkuIds));
        products.put(productId, product);
        return product;
    }

    @Override
    public Optional<ProductRecord> findProduct(long productId) {
        return Optional.ofNullable(products.get(productId));
    }

    @Override
    public List<ProductRecord> listProducts(long merchantId, long storeId, String status, String keyword) {
        return products.values().stream()
                .filter(product -> product.merchantId() == merchantId && product.storeId() == storeId)
                .filter(product -> status == null || status.isBlank() || product.status().equals(status))
                .filter(product -> matchesKeyword(product, keyword))
                .sorted(Comparator.comparingLong(ProductRecord::id))
                .toList();
    }

    @Override
    public PageResult<ProductRecord> pageProducts(long merchantId, long storeId, String status, String keyword, PageQuery pageQuery) {
        List<ProductRecord> matched = listProducts(merchantId, storeId, status, keyword);
        int fromIndex = Math.min((pageQuery.page() - 1) * pageQuery.pageSize(), matched.size());
        int toIndex = Math.min(fromIndex + pageQuery.pageSize(), matched.size());
        return new PageResult<>(matched.subList(fromIndex, toIndex), matched.size());
    }

    @Override
    public synchronized Optional<ProductRecord> updateProduct(long productId,
                                                             long categoryId,
                                                             String productName,
                                                             String productSubTitle,
                                                             List<SkuDraft> skuDrafts) {
        ProductRecord current = products.get(productId);
        if (current == null) {
            return Optional.empty();
        }
        for (Long skuId : current.skuIds()) {
            SkuRecord currentSku = skus.remove(skuId);
            if (currentSku != null) {
                merchantSkuCodeIndex.remove(merchantSkuCodeKey(currentSku.merchantId(), currentSku.skuCode()));
            }
            stocks.remove(skuId);
        }

        List<Long> skuIds = new ArrayList<>(skuDrafts.size());
        for (SkuDraft draft : skuDrafts) {
            long skuId = skuIdGenerator.incrementAndGet();
            SkuRecord sku = new SkuRecord(
                    skuId,
                    productId,
                    current.merchantId(),
                    current.storeId(),
                    draft.skuCode(),
                    draft.skuName(),
                    draft.salePrice());
            skus.put(skuId, sku);
            skuIds.add(skuId);
            stocks.put(skuId, new StockRecord(
                    skuId,
                    productId,
                    current.merchantId(),
                    current.storeId(),
                    draft.availableStock(),
                    0,
                    toStockStatus(draft.availableStock())));
            merchantSkuCodeIndex.put(merchantSkuCodeKey(current.merchantId(), draft.skuCode()), skuId);
        }

        ProductRecord updated = new ProductRecord(
                current.id(),
                current.merchantId(),
                current.storeId(),
                categoryId,
                current.productCode(),
                productName,
                productSubTitle,
                current.status(),
                List.copyOf(skuIds));
        products.put(productId, updated);
        return Optional.of(updated);
    }

    @Override
    public boolean skuCodeExists(long merchantId, String skuCode) {
        return merchantSkuCodeIndex.containsKey(merchantSkuCodeKey(merchantId, skuCode));
    }

    @Override
    public synchronized Optional<ProductRecord> updateProductStatus(long productId, String status) {
        ProductRecord current = products.get(productId);
        if (current == null) {
            return Optional.empty();
        }
        ProductRecord updated = new ProductRecord(
                current.id(),
                current.merchantId(),
                current.storeId(),
                current.categoryId(),
                current.productCode(),
                current.productName(),
                current.productSubTitle(),
                status,
                current.skuIds());
        products.put(productId, updated);
        return Optional.of(updated);
    }

    @Override
    public List<SkuRecord> listSkusByProductId(long productId) {
        ProductRecord product = products.get(productId);
        if (product == null) {
            return List.of();
        }
        List<SkuRecord> result = new ArrayList<>(product.skuIds().size());
        for (Long skuId : product.skuIds()) {
            SkuRecord sku = skus.get(skuId);
            if (sku != null) {
                result.add(sku);
            }
        }
        result.sort(Comparator.comparingLong(SkuRecord::id));
        return result;
    }

    @Override
    public Optional<SkuRecord> findSku(long skuId) {
        return Optional.ofNullable(skus.get(skuId));
    }

    @Override
    public Optional<StockRecord> findStock(long skuId) {
        return Optional.ofNullable(stocks.get(skuId));
    }

    @Override
    public synchronized Optional<StockRecord> adjustStock(long skuId, int delta, String reason) {
        StockRecord current = stocks.get(skuId);
        if (current == null) {
            return Optional.empty();
        }
        int newAvailable = current.availableStock() + delta;
        if (newAvailable < 0) {
            return Optional.empty();
        }
        StockRecord updated = new StockRecord(
                current.skuId(),
                current.productId(),
                current.merchantId(),
                current.storeId(),
                newAvailable,
                current.lockedStock(),
                toStockStatus(newAvailable),
                reason);
        stocks.put(skuId, updated);
        return Optional.of(updated);
    }

    @Override
    public ProductSalesRecord summarizePaidOrderSales(long merchantId, long storeId, long productId) {
        return new ProductSalesRecord(0, new BigDecimal("0.00"));
    }

    private Optional<CategoryRecord> findCategoryByParentAndName(long merchantId,
                                                                 long storeId,
                                                                 long parentId,
                                                                 String categoryName) {
        return categories.values().stream()
                .filter(category -> category.merchantId() == merchantId
                        && category.storeId() == storeId
                        && category.parentId() == parentId
                        && category.categoryName().equals(categoryName))
                .findFirst();
    }

    private boolean matchesKeyword(ProductRecord product, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        if (product.productCode().toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        if (String.valueOf(product.id()).contains(normalizedKeyword)) {
            return true;
        }
        if (("spu-" + product.id()).contains(normalizedKeyword)) {
            return true;
        }
        if (product.productName().toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        if (StringUtils.hasText(product.productSubTitle())
                && product.productSubTitle().toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        return listSkusByProductId(product.id()).stream().anyMatch(sku ->
                sku.skuCode().toLowerCase().contains(normalizedKeyword)
                        || sku.skuName().toLowerCase().contains(normalizedKeyword));
    }

    private void seedDefaultTemplate() {
        List<TemplateNodeDraft> drafts = List.of(
                new TemplateNodeDraft("fresh", null, "生鲜", 1, 10, true, true),
                new TemplateNodeDraft("fresh-fruit", "fresh", "水果", 2, 10, true, true),
                new TemplateNodeDraft("fresh-vegetable", "fresh", "蔬菜", 2, 20, true, true),
                new TemplateNodeDraft("drink", null, "酒水饮料", 1, 20, true, true),
                new TemplateNodeDraft("drink-tea", "drink", "茶饮", 2, 10, true, true));
        createCategoryTemplate("默认零售模板", "retail", "v1.0.0", 3, "一期默认分类模板", drafts);
    }

    private void seedDefaultMerchantCatalog() {
        long merchantId = 1001L;
        long storeId = 1001L;
        initializeCategoryTree(merchantId, storeId, 2_000_001L);
        List<CategoryRecord> defaultCategories = listCategories(merchantId, storeId, null);
        long fruitCategoryId = defaultCategories.stream()
                .filter(category -> category.categoryLevel() == 2 && "水果".equals(category.categoryName()))
                .findFirst()
                .map(CategoryRecord::id)
                .orElseThrow();
        long drinkCategoryId = defaultCategories.stream()
                .filter(category -> category.categoryLevel() == 2 && "茶饮".equals(category.categoryName()))
                .findFirst()
                .map(CategoryRecord::id)
                .orElseThrow();

        ProductRecord citrus = createProduct(
                merchantId,
                storeId,
                fruitCategoryId,
                "赣南脐橙礼盒",
                "当季现发 12 枚装",
                List.of(
                        new SkuDraft("CDD-ORANGE-001", "标准装", new BigDecimal("59.90"), 128),
                        new SkuDraft("CDD-ORANGE-002", "家庭装", new BigDecimal("89.90"), 64)));
        updateProductStatus(citrus.id(), "on_shelf");

        createProduct(
                merchantId,
                storeId,
                drinkCategoryId,
                "冷萃茉莉花茶",
                "低糖配方 6 瓶装",
                List.of(new SkuDraft("CDD-TEA-001", "尝鲜装", new BigDecimal("29.90"), 36)));

        ProductRecord coffee = createProduct(
                merchantId,
                storeId,
                drinkCategoryId,
                "挂耳美式咖啡组合",
                "工作日醒神装 20 包",
                List.of(new SkuDraft("CDD-COFFEE-001", "经典装", new BigDecimal("49.90"), 12)));
        updateProductStatus(coffee.id(), "off_shelf");
    }

    private static String merchantSkuCodeKey(long merchantId, String skuCode) {
        return merchantId + "#" + skuCode;
    }

    private static String buildProductCode(long productId) {
        return "SPU" + productId;
    }

    private static String toStockStatus(int availableStock) {
        return availableStock > 0 ? "in_stock" : "out_of_stock";
    }

}
