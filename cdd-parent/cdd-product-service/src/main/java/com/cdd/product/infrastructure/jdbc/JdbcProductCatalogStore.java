package com.cdd.product.infrastructure.jdbc;

import com.cdd.product.infrastructure.ProductCatalogStore;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcProductCatalogStore implements ProductCatalogStore {

    private static final RowMapper<CategoryRow> CATEGORY_ROW_MAPPER = JdbcProductCatalogStore::mapCategoryRow;
    private static final RowMapper<ProductRow> PRODUCT_ROW_MAPPER = JdbcProductCatalogStore::mapProductRow;
    private static final RowMapper<SkuRow> SKU_ROW_MAPPER = JdbcProductCatalogStore::mapSkuRow;
    private static final RowMapper<StockRow> STOCK_ROW_MAPPER = JdbcProductCatalogStore::mapStockRow;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong templateIdGenerator = new AtomicLong(2_000_000L);
    private final AtomicLong templateNodeIdGenerator = new AtomicLong(2_050_000L);
    private final AtomicLong categoryIdGenerator = new AtomicLong(2_100_000L);
    private final AtomicLong productIdGenerator = new AtomicLong(3_100_000L);
    private final AtomicLong skuIdGenerator = new AtomicLong(3_200_000L);
    private final AtomicLong stockIdGenerator = new AtomicLong(3_300_000L);
    private final AtomicLong stockChangeLogIdGenerator = new AtomicLong(3_400_000L);

    private final Map<Long, CategoryTemplateRecord> categoryTemplates = new HashMap<>();
    private final Map<Long, CategoryTemplateNodeRecord> categoryTemplateNodes = new HashMap<>();

    public JdbcProductCatalogStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        seedDefaultTemplate();
        initializeIdGenerators();
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
    public CategoryRecord createCategory(long merchantId,
                                         long storeId,
                                         long parentId,
                                         String categoryName,
                                         int sortOrder,
                                         boolean enabled,
                                         boolean visible,
                                         Long templateId,
                                         int categoryLevel) {
        long id = categoryIdGenerator.incrementAndGet();
        jdbcTemplate.update("""
                INSERT INTO cdd_product_category (
                  id, merchant_id, store_id, template_id, parent_id, category_name, category_level, sort_order,
                  is_enabled, is_visible, created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, merchantId, storeId, templateId, parentId, categoryName, categoryLevel, sortOrder,
                enabled ? 1 : 0, visible ? 1 : 0, merchantId, merchantId, 0, 0L);
        return new CategoryRecord(id, merchantId, storeId, templateId, parentId, categoryName, categoryLevel, sortOrder, enabled, visible);
    }

    @Override
    public Optional<CategoryRecord> findCategory(long categoryId) {
        List<CategoryRow> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, template_id, parent_id, category_name, category_level,
                       sort_order, is_enabled, is_visible
                FROM cdd_product_category
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, CATEGORY_ROW_MAPPER, categoryId);
        return rows.stream().findFirst().map(this::toCategoryRecord);
    }

    @Override
    public List<CategoryRecord> listCategories(long merchantId, long storeId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, template_id, parent_id, category_name, category_level,
                       sort_order, is_enabled, is_visible
                FROM cdd_product_category
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                ORDER BY category_level ASC, sort_order ASC, id ASC
                """, CATEGORY_ROW_MAPPER, merchantId, storeId).stream()
                .map(this::toCategoryRecord)
                .toList();
    }

    @Override
    public boolean categoryNameExists(long merchantId, long storeId, long parentId, String categoryName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_product_category
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND parent_id = ?
                  AND category_name = ?
                  AND deleted = 0
                """, Integer.class, merchantId, storeId, parentId, categoryName);
        return count != null && count > 0;
    }

    @Override
    public boolean categoryHasChildren(long merchantId, long storeId, long categoryId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_product_category
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND parent_id = ?
                  AND deleted = 0
                """, Integer.class, merchantId, storeId, categoryId);
        return count != null && count > 0;
    }

    @Override
    public boolean productExistsInCategory(long merchantId, long storeId, long categoryId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_product_spu
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND category_id = ?
                  AND deleted = 0
                """, Integer.class, merchantId, storeId, categoryId);
        return count != null && count > 0;
    }

    @Override
    public Optional<CategoryRecord> updateCategory(long categoryId,
                                                   String categoryName,
                                                   Integer sortOrder,
                                                   Boolean enabled,
                                                   Boolean visible) {
        Optional<CategoryRecord> currentOpt = findCategory(categoryId);
        if (currentOpt.isEmpty()) {
            return Optional.empty();
        }
        CategoryRecord current = currentOpt.get();
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
        jdbcTemplate.update("""
                UPDATE cdd_product_category
                SET category_name = ?, sort_order = ?, is_enabled = ?, is_visible = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                updated.categoryName(),
                updated.sortOrder(),
                updated.enabled() ? 1 : 0,
                updated.visible() ? 1 : 0,
                categoryId);
        return Optional.of(updated);
    }

    @Override
    public ProductRecord createProduct(long merchantId,
                                       long storeId,
                                       long categoryId,
                                       String productName,
                                       String productSubTitle,
                                       List<SkuDraft> skuDrafts) {
        long productId = productIdGenerator.incrementAndGet();
        String productCode = "P" + productId;
        jdbcTemplate.update("""
                INSERT INTO cdd_product_spu (
                  id, merchant_id, store_id, category_id, product_name, product_code, product_sub_title, status,
                  publish_check_status, created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                productId, merchantId, storeId, categoryId, productName, productCode, productSubTitle, "draft",
                "passed", merchantId, merchantId, 0, 0L);

        List<Long> skuIds = new ArrayList<>(skuDrafts.size());
        for (SkuDraft draft : skuDrafts) {
            long skuId = skuIdGenerator.incrementAndGet();
            long stockId = stockIdGenerator.incrementAndGet();
            skuIds.add(skuId);
            jdbcTemplate.update("""
                    INSERT INTO cdd_product_sku (
                      id, merchant_id, store_id, product_id, sku_code, sku_name, sale_price, status,
                      created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    skuId, merchantId, storeId, productId, draft.skuCode(), draft.skuName(), draft.salePrice(), "enabled",
                    merchantId, merchantId, 0, 0L);
            jdbcTemplate.update("""
                    INSERT INTO cdd_product_stock (
                      id, merchant_id, store_id, product_id, sku_id, available_stock, locked_stock, stock_status,
                      updated_reason, created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    stockId, merchantId, storeId, productId, skuId, draft.availableStock(), 0, toStockStatus(draft.availableStock()),
                    "商品创建初始化库存", merchantId, merchantId, 0, 0L);
        }
        return new ProductRecord(productId, merchantId, storeId, categoryId, productName, productSubTitle, "draft", List.copyOf(skuIds));
    }

    @Override
    public Optional<ProductRecord> findProduct(long productId) {
        List<ProductRow> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, category_id, product_name, product_sub_title, status
                FROM cdd_product_spu
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, PRODUCT_ROW_MAPPER, productId);
        return rows.stream().findFirst().map(this::toProductRecord);
    }

    @Override
    public List<ProductRecord> listProducts(long merchantId, long storeId, String status) {
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, store_id, category_id, product_name, product_sub_title, status
                    FROM cdd_product_spu
                    WHERE merchant_id = ?
                      AND store_id = ?
                      AND deleted = 0
                    ORDER BY id ASC
                    """, PRODUCT_ROW_MAPPER, merchantId, storeId).stream()
                    .map(this::toProductRecord)
                    .toList();
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, category_id, product_name, product_sub_title, status
                FROM cdd_product_spu
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND status = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, PRODUCT_ROW_MAPPER, merchantId, storeId, status).stream()
                .map(this::toProductRecord)
                .toList();
    }

    @Override
    public boolean skuCodeExists(long merchantId, String skuCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_product_sku
                WHERE merchant_id = ?
                  AND sku_code = ?
                  AND deleted = 0
                """, Integer.class, merchantId, skuCode);
        return count != null && count > 0;
    }

    @Override
    public Optional<ProductRecord> updateProductStatus(long productId, String status) {
        Optional<ProductRecord> current = findProduct(productId);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update("""
                UPDATE cdd_product_spu
                SET status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """, status, productId);
        ProductRecord record = current.get();
        return Optional.of(new ProductRecord(
                record.id(),
                record.merchantId(),
                record.storeId(),
                record.categoryId(),
                record.productName(),
                record.productSubTitle(),
                status,
                record.skuIds()));
    }

    @Override
    public List<SkuRecord> listSkusByProductId(long productId) {
        return jdbcTemplate.query("""
                SELECT id, product_id, merchant_id, store_id, sku_code, sku_name, sale_price
                FROM cdd_product_sku
                WHERE product_id = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, SKU_ROW_MAPPER, productId).stream()
                .map(this::toSkuRecord)
                .toList();
    }

    @Override
    public Optional<SkuRecord> findSku(long skuId) {
        List<SkuRow> rows = jdbcTemplate.query("""
                SELECT id, product_id, merchant_id, store_id, sku_code, sku_name, sale_price
                FROM cdd_product_sku
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, SKU_ROW_MAPPER, skuId);
        return rows.stream().findFirst().map(this::toSkuRecord);
    }

    @Override
    public Optional<StockRecord> findStock(long skuId) {
        List<StockRow> rows = jdbcTemplate.query("""
                SELECT sku_id, product_id, merchant_id, store_id, available_stock, locked_stock, stock_status, updated_reason
                FROM cdd_product_stock
                WHERE sku_id = ?
                  AND deleted = 0
                LIMIT 1
                """, STOCK_ROW_MAPPER, skuId);
        return rows.stream().findFirst().map(this::toStockRecord);
    }

    @Override
    public Optional<StockRecord> adjustStock(long skuId, int delta, String reason) {
        List<StockDbRow> rows = jdbcTemplate.query("""
                SELECT id, sku_id, product_id, merchant_id, store_id, available_stock, locked_stock
                FROM cdd_product_stock
                WHERE sku_id = ?
                  AND deleted = 0
                LIMIT 1
                """, JdbcProductCatalogStore::mapStockDbRow, skuId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        StockDbRow current = rows.get(0);
        int newAvailable = current.availableStock() + delta;
        if (newAvailable < 0) {
            return Optional.empty();
        }
        String targetStatus = toStockStatus(newAvailable);
        Instant now = Instant.now();
        jdbcTemplate.update("""
                UPDATE cdd_product_stock
                SET available_stock = ?, stock_status = ?, updated_reason = ?, last_stock_in_at = ?, last_stock_out_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                newAvailable,
                targetStatus,
                reason,
                delta > 0 ? Timestamp.from(now) : null,
                delta < 0 ? Timestamp.from(now) : null,
                current.id());
        jdbcTemplate.update("""
                INSERT INTO cdd_product_stock_change_log (
                  id, merchant_id, store_id, product_id, sku_id, change_type, delta_stock, before_available_stock,
                  after_available_stock, reason, created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                stockChangeLogIdGenerator.incrementAndGet(),
                current.merchantId(),
                current.storeId(),
                current.productId(),
                current.skuId(),
                delta >= 0 ? "manual_increase" : "manual_decrease",
                delta,
                current.availableStock(),
                newAvailable,
                reason,
                current.merchantId(),
                current.merchantId(),
                0,
                0L);
        return Optional.of(new StockRecord(
                current.skuId(),
                current.productId(),
                current.merchantId(),
                current.storeId(),
                newAvailable,
                current.lockedStock(),
                targetStatus,
                reason));
    }

    private Optional<CategoryRecord> findCategoryByParentAndName(long merchantId,
                                                                 long storeId,
                                                                 long parentId,
                                                                 String categoryName) {
        List<CategoryRow> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, template_id, parent_id, category_name, category_level,
                       sort_order, is_enabled, is_visible
                FROM cdd_product_category
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND parent_id = ?
                  AND category_name = ?
                  AND deleted = 0
                LIMIT 1
                """, CATEGORY_ROW_MAPPER, merchantId, storeId, parentId, categoryName);
        return rows.stream().findFirst().map(this::toCategoryRecord);
    }

    private void seedDefaultTemplate() {
        createCategoryTemplate("默认零售模板", "retail", "v1.0.0", 3, "一期默认分类模板", List.of(
                new TemplateNodeDraft("fresh", null, "生鲜", 1, 10, true, true),
                new TemplateNodeDraft("fresh-fruit", "fresh", "水果", 2, 10, true, true),
                new TemplateNodeDraft("fresh-vegetable", "fresh", "蔬菜", 2, 20, true, true),
                new TemplateNodeDraft("drink", null, "酒水饮料", 1, 20, true, true),
                new TemplateNodeDraft("drink-tea", "drink", "茶饮", 2, 10, true, true)));
    }

    private void initializeIdGenerators() {
        categoryIdGenerator.set(Math.max(categoryIdGenerator.get(), maxId("cdd_product_category")));
        productIdGenerator.set(Math.max(productIdGenerator.get(), maxId("cdd_product_spu")));
        skuIdGenerator.set(Math.max(skuIdGenerator.get(), maxId("cdd_product_sku")));
        stockIdGenerator.set(Math.max(stockIdGenerator.get(), maxId("cdd_product_stock")));
        stockChangeLogIdGenerator.set(Math.max(stockChangeLogIdGenerator.get(), maxId("cdd_product_stock_change_log")));
    }

    private long maxId(String tableName) {
        Long value = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + tableName, Long.class);
        return value == null ? 0L : value;
    }

    private CategoryRecord toCategoryRecord(CategoryRow row) {
        return new CategoryRecord(
                row.id(),
                row.merchantId(),
                row.storeId(),
                row.templateId(),
                row.parentId(),
                row.categoryName(),
                row.categoryLevel(),
                row.sortOrder(),
                row.enabled(),
                row.visible());
    }

    private ProductRecord toProductRecord(ProductRow row) {
        return new ProductRecord(
                row.id(),
                row.merchantId(),
                row.storeId(),
                row.categoryId(),
                row.productName(),
                row.productSubTitle(),
                row.status(),
                listSkusByProductId(row.id()).stream().map(SkuRecord::id).toList());
    }

    private SkuRecord toSkuRecord(SkuRow row) {
        return new SkuRecord(row.id(), row.productId(), row.merchantId(), row.storeId(), row.skuCode(), row.skuName(), row.salePrice());
    }

    private StockRecord toStockRecord(StockRow row) {
        return new StockRecord(row.skuId(), row.productId(), row.merchantId(), row.storeId(), row.availableStock(), row.lockedStock(), row.stockStatus(), row.lastReason());
    }

    private static CategoryRow mapCategoryRow(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryRow(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getObject("template_id", Long.class),
                rs.getLong("parent_id"),
                rs.getString("category_name"),
                rs.getInt("category_level"),
                rs.getInt("sort_order"),
                rs.getBoolean("is_enabled"),
                rs.getBoolean("is_visible"));
    }

    private static ProductRow mapProductRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProductRow(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("category_id"),
                rs.getString("product_name"),
                rs.getString("product_sub_title"),
                rs.getString("status"));
    }

    private static SkuRow mapSkuRow(ResultSet rs, int rowNum) throws SQLException {
        return new SkuRow(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("sku_code"),
                rs.getString("sku_name"),
                rs.getBigDecimal("sale_price"));
    }

    private static StockRow mapStockRow(ResultSet rs, int rowNum) throws SQLException {
        return new StockRow(
                rs.getLong("sku_id"),
                rs.getLong("product_id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getInt("available_stock"),
                rs.getInt("locked_stock"),
                rs.getString("stock_status"),
                rs.getString("updated_reason"));
    }

    private static StockDbRow mapStockDbRow(ResultSet rs, int rowNum) throws SQLException {
        return new StockDbRow(
                rs.getLong("id"),
                rs.getLong("sku_id"),
                rs.getLong("product_id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getInt("available_stock"),
                rs.getInt("locked_stock"));
    }

    private static String toStockStatus(int availableStock) {
        return availableStock > 0 ? "in_stock" : "out_of_stock";
    }

    private record CategoryRow(
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

    private record ProductRow(
            long id,
            long merchantId,
            long storeId,
            long categoryId,
            String productName,
            String productSubTitle,
            String status) {
    }

    private record SkuRow(
            long id,
            long productId,
            long merchantId,
            long storeId,
            String skuCode,
            String skuName,
            BigDecimal salePrice) {
    }

    private record StockRow(
            long skuId,
            long productId,
            long merchantId,
            long storeId,
            int availableStock,
            int lockedStock,
            String stockStatus,
            String lastReason) {
    }

    private record StockDbRow(
            long id,
            long skuId,
            long productId,
            long merchantId,
            long storeId,
            int availableStock,
            int lockedStock) {
    }
}
