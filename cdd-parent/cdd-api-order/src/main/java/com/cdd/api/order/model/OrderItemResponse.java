package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderItemResponse(
        long id,
        @JsonProperty("order_id")
        long orderId,
        @JsonProperty("product_id")
        long productId,
        @JsonProperty("sku_id")
        long skuId,
        @JsonProperty("product_name")
        String productName,
        @JsonProperty("sku_name")
        String skuName,
        @JsonProperty("sale_price")
        BigDecimal salePrice,
        int quantity,
        @JsonProperty("line_amount")
        BigDecimal lineAmount,
        @JsonProperty("refund_status")
        String refundStatus,
        @JsonProperty("refunded_quantity")
        Integer refundedQuantity,
        @JsonProperty("refunded_amount")
        BigDecimal refundedAmount) {
}
