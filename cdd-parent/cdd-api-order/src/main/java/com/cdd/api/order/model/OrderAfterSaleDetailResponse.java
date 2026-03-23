package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderAfterSaleDetailResponse(
        @JsonProperty("after_sale_no")
        String afterSaleNo,
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("order_item_id")
        Long orderItemId,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("user_id")
        long userId,
        @JsonProperty("after_sale_type")
        String afterSaleType,
        @JsonProperty("after_sale_status")
        String afterSaleStatus,
        @JsonProperty("product_name")
        String productName,
        @JsonProperty("sku_name")
        String skuName,
        @JsonProperty("refund_quantity")
        Integer refundQuantity,
        @JsonProperty("refund_amount")
        BigDecimal refundAmount,
        @JsonProperty("reason_code")
        String reasonCode,
        @JsonProperty("reason_desc")
        String reasonDesc,
        @JsonProperty("proof_urls")
        List<String> proofUrls,
        @JsonProperty("merchant_result")
        String merchantResult,
        @JsonProperty("refund_no")
        String refundNo,
        @JsonProperty("refund_status")
        String refundStatus,
        @JsonProperty("refund_failure_reason")
        String refundFailureReason,
        @JsonProperty("third_party_refund_no")
        String thirdPartyRefundNo,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("return_company")
        String returnCompany,
        @JsonProperty("return_logistics_no")
        String returnLogisticsNo,
        @JsonProperty("handled_by")
        Long handledBy,
        @JsonProperty("handled_at")
        Instant handledAt,
        @JsonProperty("approved_at")
        Instant approvedAt,
        @JsonProperty("returned_at")
        Instant returnedAt,
        @JsonProperty("refund_applied_at")
        Instant refundAppliedAt,
        @JsonProperty("refund_success_at")
        Instant refundSuccessAt,
        @JsonProperty("completed_at")
        Instant completedAt,
        @JsonProperty("closed_at")
        Instant closedAt,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt) {
}
