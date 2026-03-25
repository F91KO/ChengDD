package com.cdd.product.web;

import com.cdd.api.product.model.AdjustStockRequest;
import com.cdd.api.product.model.CategoryResponse;
import com.cdd.api.product.model.CategoryTemplateResponse;
import com.cdd.api.product.model.CreateCategoryRequest;
import com.cdd.api.product.model.CreateCategoryTemplateRequest;
import com.cdd.api.product.model.CreateProductRequest;
import com.cdd.api.product.model.InitializeCategoryTreeRequest;
import com.cdd.api.product.model.InitializeCategoryTreeResponse;
import com.cdd.api.product.model.ProductDetailResponse;
import com.cdd.api.product.model.ProductStockResponse;
import com.cdd.api.product.model.ProductSummaryResponse;
import com.cdd.api.product.model.UpdateCategoryRequest;
import com.cdd.api.product.model.UpdateProductRequest;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.product.service.ProductCatalogApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductCatalogApplicationService productCatalogApplicationService;

    public ProductController(ProductCatalogApplicationService productCatalogApplicationService) {
        this.productCatalogApplicationService = productCatalogApplicationService;
    }

    @PostMapping("/category-templates")
    public ApiResponse<CategoryTemplateResponse> createCategoryTemplate(
            @Valid @RequestBody CreateCategoryTemplateRequest request) {
        return ApiResponses.success(productCatalogApplicationService.createCategoryTemplate(request));
    }

    @GetMapping("/category-templates")
    public ApiResponse<List<CategoryTemplateResponse>> listCategoryTemplates() {
        return ApiResponses.success(productCatalogApplicationService.listCategoryTemplates());
    }

    @PostMapping("/categories/init")
    public ApiResponse<InitializeCategoryTreeResponse> initializeCategoryTree(
            @Valid @RequestBody InitializeCategoryTreeRequest request) {
        return ApiResponses.success(productCatalogApplicationService.initializeCategoryTree(request));
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponses.success(productCatalogApplicationService.createCategory(request));
    }

    @PutMapping("/categories/{category_id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable(name = "category_id") Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponses.success(productCatalogApplicationService.updateCategory(categoryId, request));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories(
            @RequestParam(name = "merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam(name = "store_id") @NotNull(message = "门店ID不能为空") Long storeId) {
        return ApiResponses.success(productCatalogApplicationService.listCategories(merchantId, storeId));
    }

    @PostMapping("/spu")
    public ApiResponse<ProductDetailResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponses.success(productCatalogApplicationService.createProduct(request));
    }

    @GetMapping("/spu/{product_id}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable(name = "product_id") Long productId) {
        return ApiResponses.success(productCatalogApplicationService.getProduct(productId));
    }

    @PutMapping("/spu/{product_id}")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable(name = "product_id") Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponses.success(productCatalogApplicationService.updateProduct(productId, request));
    }

    @GetMapping("/spu")
    public ApiResponse<List<ProductSummaryResponse>> listProducts(
            @RequestParam(name = "merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam(name = "store_id") @NotNull(message = "门店ID不能为空") Long storeId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiResponses.success(productCatalogApplicationService.listProducts(merchantId, storeId, status, keyword));
    }

    @PostMapping("/spu/{product_id}/publish")
    public ApiResponse<ProductDetailResponse> publishProduct(@PathVariable(name = "product_id") Long productId) {
        return ApiResponses.success(productCatalogApplicationService.publishProduct(productId));
    }

    @PostMapping("/spu/{product_id}/unpublish")
    public ApiResponse<ProductDetailResponse> unpublishProduct(@PathVariable(name = "product_id") Long productId) {
        return ApiResponses.success(productCatalogApplicationService.unpublishProduct(productId));
    }

    @PostMapping("/stock/adjust")
    public ApiResponse<ProductStockResponse> adjustStock(@Valid @RequestBody AdjustStockRequest request) {
        return ApiResponses.success(productCatalogApplicationService.adjustStock(request));
    }
}
