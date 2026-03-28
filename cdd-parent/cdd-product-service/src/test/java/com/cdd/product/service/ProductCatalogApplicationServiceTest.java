package com.cdd.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cdd.api.product.model.AdjustStockRequest;
import com.cdd.api.product.model.CreateProductRequest;
import com.cdd.api.product.model.CreateSkuRequest;
import com.cdd.api.product.model.InitializeCategoryTreeRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.product.ProductServiceApplication;
import com.cdd.product.error.ProductErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ProductServiceApplication.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductCatalogApplicationServiceTest {

    private static final long MERCHANT_ID = 3001L;
    private static final long STORE_ID = 4001L;
    private static final long DEFAULT_TEMPLATE_ID = 2_000_001L;
    private static final long PREMIUM_FRESH_TEMPLATE_ID = 2_000_002L;
    private static final long COMMUNITY_DELIVERY_TEMPLATE_ID = 2_000_003L;
    private static final String APPLE_SKU_CODE = "APPLE-001";

    @Autowired
    private ProductCatalogApplicationService service;

    @Test
    void shouldCreateProductAndSupportPublishUnpublishAndStockAdjust() {
        service.initializeCategoryTree(new InitializeCategoryTreeRequest(MERCHANT_ID, STORE_ID, DEFAULT_TEMPLATE_ID));
        long leafCategoryId = service.listCategories(MERCHANT_ID, STORE_ID).stream()
                .filter(category -> category.categoryLevel() == 2)
                .findFirst()
                .orElseThrow()
                .id();

        var created = service.createProduct(new CreateProductRequest(
                MERCHANT_ID,
                STORE_ID,
                leafCategoryId,
                "鲜切苹果",
                "当日现切",
                List.of(new CreateSkuRequest(
                        APPLE_SKU_CODE,
                        "500g装",
                        new BigDecimal("12.80"),
                        20))));

        assertEquals("draft", created.status());
        assertEquals(1, created.skus().size());
        long productId = created.id();
        long skuId = created.skus().get(0).id();

        var published = service.publishProduct(productId);
        assertEquals("on_shelf", published.status());

        var stock = service.adjustStock(new AdjustStockRequest(
                MERCHANT_ID,
                STORE_ID,
                productId,
                skuId,
                -5,
                "销售扣减"));
        assertEquals(15, stock.availableStock());
        assertEquals("in_stock", stock.stockStatus());

        var unpublished = service.unpublishProduct(productId);
        assertEquals("off_shelf", unpublished.status());
    }

    @Test
    void shouldRejectProductCreationWhenCategoryNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.createProduct(new CreateProductRequest(
                        MERCHANT_ID,
                        STORE_ID,
                        999999L,
                        "不存在分类商品",
                        null,
                        List.of(new CreateSkuRequest(
                                "MISS-001",
                                "默认规格",
                                new BigDecimal("1.00"),
                                1)))));
        assertEquals(ProductErrorCode.CATEGORY_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldExposeDefaultMerchantCatalogForFrontendDemo() {
        var products = service.listProducts(1001L, 1001L, null, null);

        assertEquals(3, products.size());
        assertEquals("赣南脐橙礼盒", products.get(0).productName());
        assertEquals("当季现发 12 枚装", products.get(0).productSubTitle());
        assertEquals("on_shelf", products.get(0).status());
        assertEquals(new BigDecimal("59.90"), products.get(0).priceSummary().minSalePrice());
        assertEquals(new BigDecimal("59.90"), products.get(0).priceSummary().maxSalePrice());
        assertEquals(1, products.get(0).salesSummary().totalSalesQuantity());
        assertEquals(new BigDecimal("59.90"), products.get(0).salesSummary().totalSalesAmount());
        assertEquals(128, products.get(0).stockSummary().totalAvailableStock());
        assertEquals("in_stock", products.get(0).stockSummary().stockStatus());
        assertEquals(1, products.get(0).skuSummaries().size());
        assertEquals("CDD-ORANGE-001", products.get(0).skuSummaries().get(0).skuCode());
        assertTrue(products.stream().anyMatch(product -> "draft".equals(product.status())));
        assertTrue(products.stream().anyMatch(product -> "off_shelf".equals(product.status())));
    }

    @Test
    void shouldSearchProductsByKeywordAcrossProductAndSkuFields() {
        var byProductName = service.listProducts(1001L, 1001L, null, "赣南");
        assertEquals(1, byProductName.size());
        assertEquals("CDD-ORANGE-001", byProductName.get(0).skuSummaries().get(0).skuCode());

        var bySkuCode = service.listProducts(1001L, 1001L, null, "COFFEE-001");
        assertEquals(1, bySkuCode.size());
        assertEquals("CDD-COFFEE-001", bySkuCode.get(0).skuSummaries().get(0).skuCode());
    }

    @Test
    void shouldSearchProductsByProductCodeAndLegacySpuDisplayValue() {
        var products = service.listProducts(1001L, 1001L, null, null);
        var target = products.get(0);

        var byProductCode = service.listProducts(1001L, 1001L, null, target.productCode());
        assertEquals(1, byProductCode.size());
        assertEquals(target.id(), byProductCode.get(0).id());

        var byLegacyDisplayValue = service.listProducts(1001L, 1001L, null, "SPU-" + target.id());
        assertEquals(1, byLegacyDisplayValue.size());
        assertEquals(target.id(), byLegacyDisplayValue.get(0).id());
    }

    @Test
    void shouldListCategoryTemplatesIncludingNeutralNamedTemplates() {
        var templates = service.listCategoryTemplates();

        assertTrue(templates.size() >= 3);
        assertTrue(templates.stream().anyMatch(template -> template.id() == DEFAULT_TEMPLATE_ID));
        assertTrue(templates.stream().anyMatch(template -> template.id() == PREMIUM_FRESH_TEMPLATE_ID
                && "品质精选生鲜模板".equals(template.templateName())));
        assertTrue(templates.stream().anyMatch(template -> template.id() == COMMUNITY_DELIVERY_TEMPLATE_ID
                && "社区民生到家模板".equals(template.templateName())));

        var premiumTemplate = templates.stream()
                .filter(template -> template.id() == PREMIUM_FRESH_TEMPLATE_ID)
                .findFirst()
                .orElseThrow();
        assertTrue(premiumTemplate.categories().size() > 0);

        var communityTemplate = templates.stream()
                .filter(template -> template.id() == COMMUNITY_DELIVERY_TEMPLATE_ID)
                .findFirst()
                .orElseThrow();
        assertTrue(communityTemplate.categories().size() > 0);
    }

    @Test
    void shouldInitializeCategoryTreeFromPremiumFreshTemplate() {
        var result = service.initializeCategoryTree(new InitializeCategoryTreeRequest(
                MERCHANT_ID,
                STORE_ID,
                PREMIUM_FRESH_TEMPLATE_ID));

        assertTrue(result.initializedCategoryCount() > 0);
        assertEquals(PREMIUM_FRESH_TEMPLATE_ID, result.templateId());

        var categories = service.listCategories(MERCHANT_ID, STORE_ID);
        assertTrue(categories.stream().anyMatch(category ->
                "水果鲜切".equals(category.categoryName()) && category.categoryLevel() == 1));
        assertTrue(categories.stream().anyMatch(category ->
                "应季水果".equals(category.categoryName()) && category.categoryLevel() == 2));

        var repeat = service.initializeCategoryTree(new InitializeCategoryTreeRequest(
                MERCHANT_ID,
                STORE_ID,
                PREMIUM_FRESH_TEMPLATE_ID));
        assertEquals(0, repeat.initializedCategoryCount());
        assertEquals("商家分类树已存在，无需重复初始化", repeat.message());
    }

    @Test
    void shouldSearchCategoriesByKeyword() {
        service.initializeCategoryTree(new InitializeCategoryTreeRequest(
                MERCHANT_ID,
                STORE_ID,
                PREMIUM_FRESH_TEMPLATE_ID));

        var categories = service.listCategories(MERCHANT_ID, STORE_ID, "水果");

        assertTrue(categories.size() >= 2);
        assertTrue(categories.stream().allMatch(category -> category.categoryName().contains("水果")));
        assertTrue(categories.stream().anyMatch(category -> "水果鲜切".equals(category.categoryName())));
        assertTrue(categories.stream().anyMatch(category -> "应季水果".equals(category.categoryName())));
    }
}
