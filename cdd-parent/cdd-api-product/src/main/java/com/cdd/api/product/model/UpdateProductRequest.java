package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateProductRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("category_id")
        @NotNull(message = "分类ID不能为空")
        Long categoryId,
        @JsonProperty("product_name")
        @NotBlank(message = "商品名称不能为空")
        String productName,
        @JsonProperty("product_sub_title")
        String productSubTitle,
        @JsonProperty("skus")
        @NotEmpty(message = "SKU列表不能为空")
        @Valid
        List<CreateSkuRequest> skus) {
}
