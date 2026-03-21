package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record OrderAfterSaleCreateRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("user_id")
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @JsonProperty("order_item_id")
        @NotNull(message = "订单项ID不能为空")
        Long orderItemId,
        @JsonProperty("after_sale_type")
        @NotBlank(message = "售后类型不能为空")
        String afterSaleType,
        @JsonProperty("refund_quantity")
        @NotNull(message = "退款数量不能为空")
        @Min(value = 1, message = "退款数量必须大于0")
        Integer refundQuantity,
        @JsonProperty("refund_amount")
        @NotNull(message = "退款金额不能为空")
        @DecimalMin(value = "0.01", message = "退款金额必须大于0")
        BigDecimal refundAmount,
        @JsonProperty("reason_code")
        String reasonCode,
        @JsonProperty("reason_desc")
        String reasonDesc,
        @JsonProperty("proof_urls")
        List<String> proofUrls) {
}
