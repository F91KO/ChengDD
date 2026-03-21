package com.cdd.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cdd.api.product.model.AdjustStockRequest;
import com.cdd.api.product.model.CreateProductRequest;
import com.cdd.api.product.model.CreateSkuRequest;
import com.cdd.api.product.model.InitializeCategoryTreeRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.product.error.ProductErrorCode;
import com.cdd.product.infrastructure.memory.InMemoryProductCatalogStore;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductCatalogApplicationServiceTest {

    private static final long MERCHANT_ID = 3001L;
    private static final long STORE_ID = 4001L;
    private static final long DEFAULT_TEMPLATE_ID = 2_000_001L;

    private ProductCatalogApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogApplicationService(new InMemoryProductCatalogStore());
    }

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
                        "APPLE-001",
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
}
