package com.cdd.product.service;

import com.cdd.api.product.model.AdjustStockRequest;
import com.cdd.api.product.model.CategoryResponse;
import com.cdd.api.product.model.CategoryTemplateNodeRequest;
import com.cdd.api.product.model.CategoryTemplateNodeResponse;
import com.cdd.api.product.model.CategoryTemplateResponse;
import com.cdd.api.product.model.CreateCategoryRequest;
import com.cdd.api.product.model.CreateCategoryTemplateRequest;
import com.cdd.api.product.model.CreateProductRequest;
import com.cdd.api.product.model.CreateSkuRequest;
import com.cdd.api.product.model.InitializeCategoryTreeRequest;
import com.cdd.api.product.model.InitializeCategoryTreeResponse;
import com.cdd.api.product.model.ProductDetailResponse;
import com.cdd.api.product.model.ProductPriceSummaryResponse;
import com.cdd.api.product.model.ProductSalesSummaryResponse;
import com.cdd.api.product.model.ProductSkuResponse;
import com.cdd.api.product.model.ProductStockResponse;
import com.cdd.api.product.model.ProductStockSummaryResponse;
import com.cdd.api.product.model.ProductSummaryResponse;
import com.cdd.api.product.model.UpdateCategoryRequest;
import com.cdd.api.product.model.UpdateProductRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.product.error.ProductErrorCode;
import com.cdd.product.infrastructure.ProductCatalogStore;
import com.cdd.product.support.BusinessCodeGenerator;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductCatalogApplicationService {

    private static final String PRODUCT_STATUS_DRAFT = "draft";
    private static final String PRODUCT_STATUS_ON_SHELF = "on_shelf";
    private static final String PRODUCT_STATUS_OFF_SHELF = "off_shelf";

    private final ProductCatalogStore store;
    private final BusinessCodeGenerator businessCodeGenerator;

    public ProductCatalogApplicationService(ProductCatalogStore store,
                                            BusinessCodeGenerator businessCodeGenerator) {
        this.store = store;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public CategoryTemplateResponse createCategoryTemplate(CreateCategoryTemplateRequest request) {
        String templateName = request.templateName().trim();
        String templateVersion = request.templateVersion().trim();
        String industryCode = request.industryCode().trim();
        if (store.categoryTemplateExists(templateName, templateVersion)) {
            throw new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_DUPLICATE);
        }
        Map<String, CategoryTemplateNodeRequest> nodeByCode = new HashMap<>();
        for (CategoryTemplateNodeRequest node : request.categories()) {
            String code = normalize(node.templateCategoryCode());
            if (nodeByCode.containsKey(code)) {
                throw new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_NODE_INVALID, "模板分类编码重复");
            }
            nodeByCode.put(code, node);
        }

        Map<String, Integer> levelCache = new HashMap<>();
        Map<String, String> siblingNameCheck = new HashMap<>();
        List<ProductCatalogStore.TemplateNodeDraft> drafts = request.categories().stream()
                .map(node -> {
                    String code = normalize(node.templateCategoryCode());
                    String parentCode = trimToNull(node.parentTemplateCategoryCode());
                    if (parentCode != null) {
                        parentCode = normalize(parentCode);
                        if (!nodeByCode.containsKey(parentCode)) {
                            throw new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_NODE_INVALID, "模板父级分类不存在");
                        }
                    }
                    int level = resolveLevel(code, nodeByCode, levelCache);
                    if (level > request.maxLevel()) {
                        throw new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_NODE_INVALID, "模板分类层级超过最大层级");
                    }
                    String siblingKey = (parentCode == null ? "ROOT" : parentCode) + "#" + node.categoryName().trim();
                    if (siblingNameCheck.putIfAbsent(siblingKey, code) != null) {
                        throw new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_NODE_INVALID, "同级模板分类名称重复");
                    }
                    return new ProductCatalogStore.TemplateNodeDraft(
                            code,
                            parentCode,
                            node.categoryName().trim(),
                            level,
                            node.sortOrder() == null ? 0 : node.sortOrder(),
                            node.enabled() == null || node.enabled(),
                            node.visible() == null || node.visible());
                })
                .toList();

        ProductCatalogStore.CategoryTemplateRecord created = store.createCategoryTemplate(
                templateName,
                industryCode,
                templateVersion,
                request.maxLevel(),
                trimToNull(request.templateDesc()),
                drafts);
        return toCategoryTemplateResponse(created);
    }

    public List<CategoryTemplateResponse> listCategoryTemplates() {
        return store.listCategoryTemplates().stream()
                .map(this::toCategoryTemplateResponse)
                .toList();
    }

    public InitializeCategoryTreeResponse initializeCategoryTree(InitializeCategoryTreeRequest request) {
        ProductCatalogStore.CategoryTemplateRecord template = store.findCategoryTemplate(request.templateId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_NOT_FOUND));
        ProductCatalogStore.InitializeResult result = store.initializeCategoryTree(
                request.merchantId(),
                request.storeId(),
                request.templateId());
        String message = result.initializedCategoryCount() > 0
                ? "商家分类树初始化完成"
                : "商家分类树已存在，无需重复初始化";
        return new InitializeCategoryTreeResponse(
                request.merchantId(),
                request.storeId(),
                template.id(),
                result.initializedCategoryCount(),
                message);
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        long parentId = request.parentId() == null ? 0L : request.parentId();
        int categoryLevel = 1;
        if (parentId > 0L) {
            ProductCatalogStore.CategoryRecord parent = requireCategory(parentId);
            if (parent.merchantId() != request.merchantId() || parent.storeId() != request.storeId()) {
                throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
            }
            categoryLevel = parent.categoryLevel() + 1;
        }
        if (store.categoryNameExists(request.merchantId(), request.storeId(), parentId, request.categoryName().trim())) {
            throw new BusinessException(ProductErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        ProductCatalogStore.CategoryRecord created = store.createCategory(
                request.merchantId(),
                request.storeId(),
                parentId,
                request.categoryName().trim(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.enabled() == null || request.enabled(),
                request.visible() == null || request.visible(),
                null,
                categoryLevel);
        return toCategoryResponse(created);
    }

    public CategoryResponse updateCategory(long categoryId, UpdateCategoryRequest request) {
        ProductCatalogStore.CategoryRecord category = requireCategory(categoryId);
        if (category.merchantId() != request.merchantId() || category.storeId() != request.storeId()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
        }
        String categoryName = trimToNull(request.categoryName());
        if (categoryName == null
                && request.sortOrder() == null
                && request.enabled() == null
                && request.visible() == null) {
            throw new BusinessException(ProductErrorCode.CATEGORY_UPDATE_EMPTY);
        }
        if (categoryName != null) {
            boolean duplicate = store.listCategories(request.merchantId(), request.storeId()).stream()
                    .anyMatch(current -> current.id() != categoryId
                            && current.parentId() == category.parentId()
                            && current.categoryName().equals(categoryName));
            if (duplicate) {
                throw new BusinessException(ProductErrorCode.CATEGORY_NAME_DUPLICATE);
            }
        }
        if (Boolean.FALSE.equals(request.enabled())
                && category.enabled()
                && store.productExistsInCategory(request.merchantId(), request.storeId(), categoryId)) {
            throw new BusinessException(ProductErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        ProductCatalogStore.CategoryRecord updated = store.updateCategory(
                        categoryId,
                        categoryName,
                        request.sortOrder(),
                        request.enabled(),
                        request.visible())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
        return toCategoryResponse(updated);
    }

    public List<CategoryResponse> listCategories(long merchantId, long storeId) {
        return store.listCategories(merchantId, storeId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        validateProductCategory(request.merchantId(), request.storeId(), request.categoryId());
        List<ProductCatalogStore.SkuDraft> skuDrafts = buildSkuDrafts(request.merchantId(), request.skus(), Set.of());

        ProductCatalogStore.ProductRecord created = store.createProduct(
                request.merchantId(),
                request.storeId(),
                request.categoryId(),
                request.productName().trim(),
                trimToNull(request.productSubTitle()),
                skuDrafts);

        return toProductDetailResponse(created);
    }

    public ProductDetailResponse getProduct(long productId) {
        return toProductDetailResponse(requireProduct(productId));
    }

    @Transactional
    public ProductDetailResponse updateProduct(long productId, UpdateProductRequest request) {
        ProductCatalogStore.ProductRecord current = requireProduct(productId);
        if (current.merchantId() != request.merchantId() || current.storeId() != request.storeId()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        validateProductCategory(request.merchantId(), request.storeId(), request.categoryId());
        Set<String> ownedSkuCodes = store.listSkusByProductId(productId).stream()
                .map(ProductCatalogStore.SkuRecord::skuCode)
                .map(ProductCatalogApplicationService::normalize)
                .collect(Collectors.toSet());
        List<ProductCatalogStore.SkuDraft> skuDrafts = buildSkuDrafts(request.merchantId(), request.skus(), ownedSkuCodes);

        ProductCatalogStore.ProductRecord updated = store.updateProduct(
                        productId,
                        request.categoryId(),
                        request.productName().trim(),
                        trimToNull(request.productSubTitle()),
                        skuDrafts)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return toProductDetailResponse(updated);
    }

    public List<ProductSummaryResponse> listProducts(long merchantId, long storeId, String status, String keyword) {
        return store.listProducts(merchantId, storeId, status, trimToNull(keyword)).stream()
                .map(this::toProductSummaryResponse)
                .toList();
    }

    public ProductDetailResponse publishProduct(long productId) {
        ProductCatalogStore.ProductRecord product = requireProduct(productId);
        if (PRODUCT_STATUS_ON_SHELF.equals(product.status())) {
            return toProductDetailResponse(product);
        }
        if (!PRODUCT_STATUS_DRAFT.equals(product.status()) && !PRODUCT_STATUS_OFF_SHELF.equals(product.status())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_STATUS_INVALID);
        }
        ProductCatalogStore.ProductRecord updated = store.updateProductStatus(productId, PRODUCT_STATUS_ON_SHELF)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return toProductDetailResponse(updated);
    }

    public ProductDetailResponse unpublishProduct(long productId) {
        ProductCatalogStore.ProductRecord product = requireProduct(productId);
        if (PRODUCT_STATUS_OFF_SHELF.equals(product.status())) {
            return toProductDetailResponse(product);
        }
        if (!PRODUCT_STATUS_ON_SHELF.equals(product.status())) {
            throw new BusinessException(ProductErrorCode.PRODUCT_STATUS_INVALID);
        }
        ProductCatalogStore.ProductRecord updated = store.updateProductStatus(productId, PRODUCT_STATUS_OFF_SHELF)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return toProductDetailResponse(updated);
    }

    public ProductStockResponse adjustStock(AdjustStockRequest request) {
        ProductCatalogStore.ProductRecord product = requireProduct(request.productId());
        if (product.merchantId() != request.merchantId() || product.storeId() != request.storeId()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductCatalogStore.SkuRecord sku = store.findSku(request.skuId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
        if (sku.productId() != request.productId()
                || sku.merchantId() != request.merchantId()
                || sku.storeId() != request.storeId()) {
            throw new BusinessException(ProductErrorCode.SKU_NOT_FOUND);
        }
        ProductCatalogStore.StockRecord stock = store.adjustStock(request.skuId(), request.deltaStock(), request.reason())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.INVALID_STOCK_CHANGE));
        return new ProductStockResponse(
                stock.productId(),
                stock.skuId(),
                stock.availableStock(),
                stock.lockedStock(),
                stock.stockStatus());
    }

    private ProductCatalogStore.CategoryRecord requireCategory(long categoryId) {
        return store.findCategory(categoryId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
    }

    private ProductCatalogStore.ProductRecord requireProduct(long productId) {
        return store.findProduct(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateProductCategory(long merchantId, long storeId, long categoryId) {
        ProductCatalogStore.CategoryRecord category = requireCategory(categoryId);
        if (category.merchantId() != merchantId || category.storeId() != storeId) {
            throw new BusinessException(ProductErrorCode.PRODUCT_CATEGORY_MISMATCH);
        }
        if (!category.enabled()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_DISABLED);
        }
        if (store.categoryHasChildren(merchantId, storeId, category.id())) {
            throw new BusinessException(ProductErrorCode.CATEGORY_HAS_CHILDREN);
        }
    }

    private List<ProductCatalogStore.SkuDraft> buildSkuDrafts(long merchantId,
                                                              List<CreateSkuRequest> skus,
                                                              Set<String> ownedSkuCodes) {
        Set<String> requestSkuCodes = new HashSet<>();
        return skus.stream()
                .map(sku -> {
                    String skuCode = trimToNull(sku.skuCode());
                    if (!StringUtils.hasText(skuCode)) {
                        skuCode = generateSkuCode(merchantId, requestSkuCodes, ownedSkuCodes);
                    }
                    String normalizedSkuCode = normalize(skuCode);
                    if (!requestSkuCodes.add(normalizedSkuCode)) {
                        throw new BusinessException(ProductErrorCode.SKU_CODE_DUPLICATE, "SKU编码重复");
                    }
                    if (store.skuCodeExists(merchantId, skuCode) && !ownedSkuCodes.contains(normalizedSkuCode)) {
                        throw new BusinessException(ProductErrorCode.SKU_CODE_DUPLICATE);
                    }
                    if (sku.availableStock() < 0) {
                        throw new BusinessException(ProductErrorCode.INVALID_STOCK_CHANGE);
                    }
                    return new ProductCatalogStore.SkuDraft(
                            skuCode,
                            sku.skuName().trim(),
                            sku.salePrice(),
                            sku.availableStock());
                })
                .toList();
    }

    private String generateSkuCode(long merchantId, Set<String> requestSkuCodes, Set<String> ownedSkuCodes) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String skuCode = businessCodeGenerator.nextSkuCode();
            String normalizedSkuCode = normalize(skuCode);
            if (requestSkuCodes.contains(normalizedSkuCode)) {
                continue;
            }
            if (ownedSkuCodes.contains(normalizedSkuCode)) {
                continue;
            }
            if (store.skuCodeExists(merchantId, skuCode)) {
                continue;
            }
            return skuCode;
        }
        throw new BusinessException(ProductErrorCode.SKU_CODE_DUPLICATE, "SKU编码生成失败，请重试");
    }

    private CategoryResponse toCategoryResponse(ProductCatalogStore.CategoryRecord category) {
        return new CategoryResponse(
                category.id(),
                category.merchantId(),
                category.storeId(),
                category.templateId(),
                category.parentId(),
                category.categoryName(),
                category.categoryLevel(),
                category.sortOrder(),
                category.enabled(),
                category.visible());
    }

    private CategoryTemplateResponse toCategoryTemplateResponse(ProductCatalogStore.CategoryTemplateRecord template) {
        List<CategoryTemplateNodeResponse> nodes = store.listTemplateNodes(template.id()).stream()
                .map(node -> new CategoryTemplateNodeResponse(
                        node.id(),
                        node.templateCategoryCode(),
                        node.parentTemplateCategoryCode(),
                        node.categoryName(),
                        node.categoryLevel(),
                        node.sortOrder(),
                        node.enabled(),
                        node.visible()))
                .toList();
        return new CategoryTemplateResponse(
                template.id(),
                template.templateName(),
                template.industryCode(),
                template.templateVersion(),
                template.maxLevel(),
                template.status(),
                template.templateDesc(),
                nodes);
    }

    private ProductDetailResponse toProductDetailResponse(ProductCatalogStore.ProductRecord product) {
        List<ProductSkuResponse> skuResponses = buildProductSkuResponses(product);
        return new ProductDetailResponse(
                product.id(),
                product.merchantId(),
                product.storeId(),
                product.categoryId(),
                product.productCode(),
                product.productName(),
                product.productSubTitle(),
                product.status(),
                skuResponses);
    }

    private ProductSummaryResponse toProductSummaryResponse(ProductCatalogStore.ProductRecord product) {
        List<ProductSkuResponse> skuSummaries = buildProductSkuResponses(product);
        ProductCatalogStore.ProductSalesRecord salesRecord = store.summarizePaidOrderSales(
                product.merchantId(),
                product.storeId(),
                product.id());
        ProductPriceSummaryResponse priceSummary = summarizePrice(skuSummaries);
        ProductStockSummaryResponse stockSummary = summarizeStock(skuSummaries);
        return new ProductSummaryResponse(
                product.id(),
                product.merchantId(),
                product.storeId(),
                product.categoryId(),
                product.productCode(),
                product.productName(),
                product.productSubTitle(),
                product.status(),
                skuSummaries.size(),
                priceSummary,
                new ProductSalesSummaryResponse(
                        salesRecord.totalSalesQuantity(),
                        salesRecord.totalSalesAmount()),
                stockSummary,
                skuSummaries);
    }

    private List<ProductSkuResponse> buildProductSkuResponses(ProductCatalogStore.ProductRecord product) {
        return store.listSkusByProductId(product.id()).stream()
                .map(sku -> {
                    ProductCatalogStore.StockRecord stock = store.findStock(sku.id()).orElseThrow(
                            () -> new BusinessException(ProductErrorCode.SKU_NOT_FOUND));
                    return new ProductSkuResponse(
                            sku.id(),
                            sku.productId(),
                            sku.skuCode(),
                            sku.skuName(),
                            sku.salePrice(),
                            stock.availableStock(),
                            stock.lockedStock(),
                            stock.stockStatus());
                })
                .toList();
    }

    private ProductPriceSummaryResponse summarizePrice(List<ProductSkuResponse> skuSummaries) {
        if (skuSummaries.isEmpty()) {
            return new ProductPriceSummaryResponse(zeroAmount(), zeroAmount());
        }
        BigDecimal minSalePrice = skuSummaries.stream()
                .map(ProductSkuResponse::salePrice)
                .min(BigDecimal::compareTo)
                .orElseGet(ProductCatalogApplicationService::zeroAmount);
        BigDecimal maxSalePrice = skuSummaries.stream()
                .map(ProductSkuResponse::salePrice)
                .max(BigDecimal::compareTo)
                .orElseGet(ProductCatalogApplicationService::zeroAmount);
        return new ProductPriceSummaryResponse(minSalePrice, maxSalePrice);
    }

    private ProductStockSummaryResponse summarizeStock(List<ProductSkuResponse> skuSummaries) {
        int totalAvailableStock = skuSummaries.stream()
                .mapToInt(ProductSkuResponse::availableStock)
                .sum();
        int totalLockedStock = skuSummaries.stream()
                .mapToInt(ProductSkuResponse::lockedStock)
                .sum();
        return new ProductStockSummaryResponse(
                totalAvailableStock,
                totalLockedStock,
                totalAvailableStock > 0 ? "in_stock" : "out_of_stock");
    }

    private static BigDecimal zeroAmount() {
        return new BigDecimal("0.00");
    }

    private static int resolveLevel(String code,
                                    Map<String, CategoryTemplateNodeRequest> nodeByCode,
                                    Map<String, Integer> cache) {
        Integer cached = cache.get(code);
        if (cached != null) {
            return cached;
        }
        CategoryTemplateNodeRequest current = nodeByCode.get(code);
        if (current == null) {
            throw new BusinessException(ProductErrorCode.CATEGORY_TEMPLATE_NODE_INVALID);
        }
        String parentCode = trimToNull(current.parentTemplateCategoryCode());
        int level;
        if (parentCode == null) {
            level = 1;
        } else {
            level = resolveLevel(normalize(parentCode), nodeByCode, cache) + 1;
        }
        cache.put(code, level);
        return level;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }
}
