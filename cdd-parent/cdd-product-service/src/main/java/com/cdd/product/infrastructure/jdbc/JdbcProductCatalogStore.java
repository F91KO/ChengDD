package com.cdd.product.infrastructure.jdbc;

import com.cdd.common.core.page.PageQuery;
import com.cdd.common.core.page.PageResult;
import com.cdd.product.infrastructure.ProductCatalogStore;
import com.cdd.product.support.BusinessCodeGenerator;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private static final RowMapper<TemplateRow> TEMPLATE_ROW_MAPPER = JdbcProductCatalogStore::mapTemplateRow;
    private static final RowMapper<TemplateNodeRow> TEMPLATE_NODE_ROW_MAPPER = JdbcProductCatalogStore::mapTemplateNodeRow;
    private static final RowMapper<ProductRow> PRODUCT_ROW_MAPPER = JdbcProductCatalogStore::mapProductRow;
    private static final RowMapper<SkuRow> SKU_ROW_MAPPER = JdbcProductCatalogStore::mapSkuRow;
    private static final RowMapper<StockRow> STOCK_ROW_MAPPER = JdbcProductCatalogStore::mapStockRow;

    private final JdbcTemplate jdbcTemplate;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final AtomicLong templateIdGenerator = new AtomicLong(2_000_000L);
    private final AtomicLong templateNodeIdGenerator = new AtomicLong(2_050_000L);
    private final AtomicLong categoryIdGenerator = new AtomicLong(2_100_000L);
    private final AtomicLong productIdGenerator = new AtomicLong(3_100_000L);
    private final AtomicLong skuIdGenerator = new AtomicLong(3_200_000L);
    private final AtomicLong stockIdGenerator = new AtomicLong(3_300_000L);
    private final AtomicLong stockChangeLogIdGenerator = new AtomicLong(3_400_000L);

    public JdbcProductCatalogStore(JdbcTemplate jdbcTemplate, BusinessCodeGenerator businessCodeGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.businessCodeGenerator = businessCodeGenerator;
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
        jdbcTemplate.update("""
                INSERT INTO cdd_product_category_template (
                  id, template_name, industry_code, template_version, max_level, status, template_desc,
                  created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                templateId,
                templateName,
                industryCode,
                templateVersion,
                maxLevel,
                "enabled",
                templateDesc,
                0L,
                0L,
                0,
                0L);
        List<Long> nodeIds = new ArrayList<>(nodeDrafts.size());
        for (TemplateNodeDraft draft : nodeDrafts) {
            long nodeId = templateNodeIdGenerator.incrementAndGet();
            jdbcTemplate.update("""
                    INSERT INTO cdd_product_category_template_node (
                      id, template_id, template_category_code, parent_template_category_code, category_name,
                      category_level, sort_order, is_enabled, is_visible, status, created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    nodeId,
                    templateId,
                    draft.templateCategoryCode(),
                    draft.parentTemplateCategoryCode(),
                    draft.categoryName(),
                    draft.categoryLevel(),
                    draft.sortOrder(),
                    draft.enabled() ? 1 : 0,
                    draft.visible() ? 1 : 0,
                    "enabled",
                    0L,
                    0L,
                    0,
                    0L);
            nodeIds.add(nodeId);
        }
        return new CategoryTemplateRecord(
                templateId,
                templateName,
                industryCode,
                templateVersion,
                maxLevel,
                "enabled",
                templateDesc,
                List.copyOf(nodeIds));
    }

    @Override
    public Optional<CategoryTemplateRecord> findCategoryTemplate(long templateId) {
        List<TemplateRow> rows = jdbcTemplate.query("""
                SELECT id, template_name, industry_code, template_version, max_level, status, template_desc
                FROM cdd_product_category_template
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, TEMPLATE_ROW_MAPPER, templateId);
        return rows.stream().findFirst().map(this::toCategoryTemplateRecord);
    }

    @Override
    public List<CategoryTemplateRecord> listCategoryTemplates() {
        return jdbcTemplate.query("""
                SELECT id, template_name, industry_code, template_version, max_level, status, template_desc
                FROM cdd_product_category_template
                WHERE deleted = 0
                ORDER BY id ASC
                """, TEMPLATE_ROW_MAPPER).stream()
                .map(this::toCategoryTemplateRecord)
                .toList();
    }

    @Override
    public List<CategoryTemplateNodeRecord> listTemplateNodes(long templateId) {
        return jdbcTemplate.query("""
                SELECT id, template_id, template_category_code, parent_template_category_code, category_name,
                       category_level, sort_order, is_enabled, is_visible, status
                FROM cdd_product_category_template_node
                WHERE template_id = ?
                  AND deleted = 0
                ORDER BY category_level ASC, sort_order ASC, id ASC
                """, TEMPLATE_NODE_ROW_MAPPER, templateId).stream()
                .map(this::toCategoryTemplateNodeRecord)
                .toList();
    }

    @Override
    public boolean categoryTemplateExists(String templateName, String templateVersion) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_product_category_template
                WHERE template_name = ?
                  AND template_version = ?
                  AND deleted = 0
                """, Integer.class, templateName, templateVersion);
        return count != null && count > 0;
    }

    @Override
    public synchronized InitializeResult initializeCategoryTree(long merchantId, long storeId, long templateId) {
        Optional<CategoryTemplateRecord> template = findCategoryTemplate(templateId);
        if (template.isEmpty()) {
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
                    template.get().id(),
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
    public List<CategoryRecord> listCategories(long merchantId, long storeId, String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, merchant_id, store_id, template_id, parent_id, category_name, category_level,
                       sort_order, is_enabled, is_visible
                FROM cdd_product_category
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(storeId);
        if (StringUtils.hasText(keyword)) {
            sql.append("""
                      AND LOWER(category_name) LIKE ?
                    """);
            args.add("%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
        }
        sql.append("""
                ORDER BY category_level ASC, sort_order ASC, id ASC
                """);
        return jdbcTemplate.query(sql.toString(), CATEGORY_ROW_MAPPER, args.toArray()).stream()
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
        String productCode = businessCodeGenerator.nextProductCode();
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
        return new ProductRecord(productId, merchantId, storeId, categoryId, productCode, productName, productSubTitle, "draft", List.copyOf(skuIds));
    }

    @Override
    public Optional<ProductRecord> findProduct(long productId) {
        List<ProductRow> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, category_id, product_code, product_name, product_sub_title, status
                FROM cdd_product_spu
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, PRODUCT_ROW_MAPPER, productId);
        return rows.stream().findFirst().map(this::toProductRecord);
    }

    @Override
    public List<ProductRecord> listProducts(long merchantId, long storeId, String status, String keyword) {
        ProductListQuery query = buildProductListQuery(merchantId, storeId, status, keyword);
        String sql = """
                SELECT id, merchant_id, store_id, category_id, product_code, product_name, product_sub_title, status
                FROM cdd_product_spu
                """
                + query.whereClause()
                + """
                ORDER BY id ASC
                """;
        return jdbcTemplate.query(sql, PRODUCT_ROW_MAPPER, query.args().toArray()).stream()
                .map(this::toProductRecord)
                .toList();
    }

    @Override
    public PageResult<ProductRecord> pageProducts(long merchantId, long storeId, String status, String keyword, PageQuery pageQuery) {
        ProductListQuery query = buildProductListQuery(merchantId, storeId, status, keyword);
        long total = countProducts(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0);
        }

        List<Object> pageArgs = new ArrayList<>(query.args());
        pageArgs.add(pageQuery.pageSize());
        pageArgs.add((pageQuery.page() - 1) * pageQuery.pageSize());
        String sql = """
                SELECT id, merchant_id, store_id, category_id, product_code, product_name, product_sub_title, status
                FROM cdd_product_spu
                """
                + query.whereClause()
                + """
                ORDER BY id ASC
                LIMIT ? OFFSET ?
                """;
        return new PageResult<>(
                jdbcTemplate.query(sql, PRODUCT_ROW_MAPPER, pageArgs.toArray()).stream()
                .map(this::toProductRecord)
                .toList(),
                total);
    }

    private ProductListQuery buildProductListQuery(long merchantId, long storeId, String status, String keyword) {
        StringBuilder whereClause = new StringBuilder("""
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(storeId);

        if (StringUtils.hasText(status)) {
            whereClause.append("""
                      AND status = ?
                    """);
            args.add(status);
        }
        if (StringUtils.hasText(keyword)) {
            whereClause.append("""
                      AND (
                        LOWER(COALESCE(product_code, '')) LIKE ?
                        OR CAST(id AS CHAR) LIKE ?
                        OR LOWER(CONCAT('spu-', id)) LIKE ?
                        OR LOWER(product_name) LIKE ?
                        OR LOWER(COALESCE(product_sub_title, '')) LIKE ?
                        OR EXISTS (
                          SELECT 1
                          FROM cdd_product_sku sku
                          WHERE sku.product_id = cdd_product_spu.id
                            AND sku.deleted = 0
                            AND (
                              LOWER(sku.sku_code) LIKE ?
                              OR LOWER(sku.sku_name) LIKE ?
                            )
                        )
                      )
                    """);
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
        }

        return new ProductListQuery(whereClause.toString(), List.copyOf(args));
    }

    private long countProducts(ProductListQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM cdd_product_spu
                """
                + query.whereClause(), Long.class, query.args().toArray());
        return total == null ? 0 : total;
    }

    @Override
    public Optional<ProductRecord> updateProduct(long productId,
                                                 long categoryId,
                                                 String productName,
                                                 String productSubTitle,
                                                 List<SkuDraft> skuDrafts) {
        Optional<ProductRecord> current = findProduct(productId);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        ProductRecord existing = current.get();
        jdbcTemplate.update("""
                UPDATE cdd_product_spu
                SET category_id = ?, product_name = ?, product_sub_title = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                categoryId,
                productName,
                productSubTitle,
                productId);
        jdbcTemplate.update("""
                DELETE FROM cdd_product_stock
                WHERE product_id = ?
                  AND deleted <> 0
                """,
                productId);
        jdbcTemplate.update("""
                DELETE FROM cdd_product_sku
                WHERE product_id = ?
                  AND deleted <> 0
                """,
                productId);
        Map<String, SkuRecord> existingSkuByCode = new HashMap<>();
        for (SkuRecord sku : listSkusByProductId(productId)) {
            existingSkuByCode.put(normalizeSkuCode(sku.skuCode()), sku);
        }
        List<Long> skuIds = new ArrayList<>(skuDrafts.size());
        for (SkuDraft draft : skuDrafts) {
            SkuRecord existingSku = existingSkuByCode.remove(normalizeSkuCode(draft.skuCode()));
            if (existingSku != null) {
                long skuId = existingSku.id();
                skuIds.add(skuId);
                jdbcTemplate.update("""
                        UPDATE cdd_product_sku
                        SET sku_name = ?, sale_price = ?, status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                          AND deleted = 0
                        """,
                        draft.skuName(),
                        draft.salePrice(),
                        "enabled",
                        existing.merchantId(),
                        skuId);
                int updatedStockRows = jdbcTemplate.update("""
                        UPDATE cdd_product_stock
                        SET available_stock = ?, stock_status = ?, updated_reason = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE sku_id = ?
                          AND deleted = 0
                        """,
                        draft.availableStock(),
                        toStockStatus(draft.availableStock()),
                        "商品编辑更新库存",
                        existing.merchantId(),
                        skuId);
                if (updatedStockRows == 0) {
                    long stockId = stockIdGenerator.incrementAndGet();
                    jdbcTemplate.update("""
                            INSERT INTO cdd_product_stock (
                              id, merchant_id, store_id, product_id, sku_id, available_stock, locked_stock, stock_status,
                              updated_reason, created_by, updated_by, deleted, version
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                            stockId,
                            existing.merchantId(),
                            existing.storeId(),
                            productId,
                            skuId,
                            draft.availableStock(),
                            0,
                            toStockStatus(draft.availableStock()),
                            "商品编辑补建库存",
                            existing.merchantId(),
                            existing.merchantId(),
                            0,
                            0L);
                }
                continue;
            }

            long skuId = skuIdGenerator.incrementAndGet();
            long stockId = stockIdGenerator.incrementAndGet();
            skuIds.add(skuId);
            jdbcTemplate.update("""
                    INSERT INTO cdd_product_sku (
                      id, merchant_id, store_id, product_id, sku_code, sku_name, sale_price, status,
                      created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    skuId,
                    existing.merchantId(),
                    existing.storeId(),
                    productId,
                    draft.skuCode(),
                    draft.skuName(),
                    draft.salePrice(),
                    "enabled",
                    existing.merchantId(),
                    existing.merchantId(),
                    0,
                    0L);
            jdbcTemplate.update("""
                    INSERT INTO cdd_product_stock (
                      id, merchant_id, store_id, product_id, sku_id, available_stock, locked_stock, stock_status,
                      updated_reason, created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    stockId,
                    existing.merchantId(),
                    existing.storeId(),
                    productId,
                    skuId,
                    draft.availableStock(),
                    0,
                    toStockStatus(draft.availableStock()),
                    "商品编辑重建库存",
                    existing.merchantId(),
                    existing.merchantId(),
                    0,
                    0L);
        }
        for (SkuRecord removedSku : existingSkuByCode.values()) {
            jdbcTemplate.update("""
                    UPDATE cdd_product_stock
                    SET deleted = 1, updated_at = CURRENT_TIMESTAMP
                    WHERE sku_id = ?
                      AND deleted = 0
                    """,
                    removedSku.id());
            jdbcTemplate.update("""
                    UPDATE cdd_product_sku
                    SET deleted = 1, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND deleted = 0
                    """,
                    removedSku.id());
        }
        return Optional.of(new ProductRecord(
                existing.id(),
                existing.merchantId(),
                existing.storeId(),
                categoryId,
                existing.productCode(),
                productName,
                productSubTitle,
                existing.status(),
                List.copyOf(skuIds)));
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
                record.productCode(),
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

    @Override
    public ProductSalesRecord summarizePaidOrderSales(long merchantId, long storeId, long productId) {
        return jdbcTemplate.query("""
                SELECT COALESCE(SUM(GREATEST(i.quantity - COALESCE(i.refunded_quantity, 0), 0)), 0) AS total_sales_quantity,
                       COALESCE(SUM(i.line_amount - COALESCE(i.refunded_amount, 0.00)), 0.00) AS total_sales_amount
                FROM cdd_order_item i
                INNER JOIN cdd_order_info o ON o.id = i.order_id
                WHERE i.merchant_id = ?
                  AND i.store_id = ?
                  AND i.product_id = ?
                  AND i.deleted = 0
                  AND o.deleted = 0
                  AND o.pay_status = 'paid'
                """, rs -> {
            if (!rs.next()) {
                return new ProductSalesRecord(0, BigDecimal.ZERO.setScale(2));
            }
            return new ProductSalesRecord(
                    rs.getInt("total_sales_quantity"),
                    rs.getBigDecimal("total_sales_amount"));
        }, merchantId, storeId, productId);
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

    private void initializeIdGenerators() {
        templateIdGenerator.set(Math.max(templateIdGenerator.get(), maxId("cdd_product_category_template")));
        templateNodeIdGenerator.set(Math.max(templateNodeIdGenerator.get(), maxId("cdd_product_category_template_node")));
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

    private static String normalizeSkuCode(String skuCode) {
        return skuCode == null ? "" : skuCode.trim().toLowerCase(Locale.ROOT);
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

    private CategoryTemplateRecord toCategoryTemplateRecord(TemplateRow row) {
        List<Long> nodeIds = listTemplateNodes(row.id()).stream()
                .map(CategoryTemplateNodeRecord::id)
                .toList();
        return new CategoryTemplateRecord(
                row.id(),
                row.templateName(),
                row.industryCode(),
                row.templateVersion(),
                row.maxLevel(),
                row.status(),
                row.templateDesc(),
                nodeIds);
    }

    private CategoryTemplateNodeRecord toCategoryTemplateNodeRecord(TemplateNodeRow row) {
        return new CategoryTemplateNodeRecord(
                row.id(),
                row.templateId(),
                row.templateCategoryCode(),
                row.parentTemplateCategoryCode(),
                row.categoryName(),
                row.categoryLevel(),
                row.sortOrder(),
                row.enabled(),
                row.visible(),
                row.status());
    }

    private ProductRecord toProductRecord(ProductRow row) {
        return new ProductRecord(
                row.id(),
                row.merchantId(),
                row.storeId(),
                row.categoryId(),
                row.productCode(),
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

    private static TemplateRow mapTemplateRow(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateRow(
                rs.getLong("id"),
                rs.getString("template_name"),
                rs.getString("industry_code"),
                rs.getString("template_version"),
                rs.getInt("max_level"),
                rs.getString("status"),
                rs.getString("template_desc"));
    }

    private static TemplateNodeRow mapTemplateNodeRow(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateNodeRow(
                rs.getLong("id"),
                rs.getLong("template_id"),
                rs.getString("template_category_code"),
                rs.getString("parent_template_category_code"),
                rs.getString("category_name"),
                rs.getInt("category_level"),
                rs.getInt("sort_order"),
                rs.getBoolean("is_enabled"),
                rs.getBoolean("is_visible"),
                rs.getString("status"));
    }

    private static ProductRow mapProductRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProductRow(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("category_id"),
                rs.getString("product_code"),
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

    private record TemplateRow(
            long id,
            String templateName,
            String industryCode,
            String templateVersion,
            int maxLevel,
            String status,
            String templateDesc) {
    }

    private record TemplateNodeRow(
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

    private record ProductRow(
            long id,
            long merchantId,
            long storeId,
            long categoryId,
            String productCode,
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

    private record ProductListQuery(
            String whereClause,
            List<Object> args) {
    }
}
