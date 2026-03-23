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
                "售卖扣减"));
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
        var products = service.listProducts(1001L, 1001L, null);

        assertEquals(3, products.size());
        assertEquals("赣南脐橙礼盒", products.get(0).productName());
        assertEquals("on_shelf", products.get(0).status());
        assertTrue(products.stream().anyMatch(product -> "draft".equals(product.status())));
        assertTrue(products.stream().anyMatch(product -> "off_shelf".equals(product.status())));
    }
}
