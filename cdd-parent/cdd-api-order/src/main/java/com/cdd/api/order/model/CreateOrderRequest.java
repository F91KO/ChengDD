package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @JsonProperty("snapshot_token")
        @NotBlank(message = "结算快照令牌不能为空")
        String snapshotToken,
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("user_id")
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @JsonProperty("buyer_remark")
        String buyerRemark,
        @JsonProperty("receiver_name")
        String receiverName,
        @JsonProperty("receiver_mobile")
        String receiverMobile,
        @JsonProperty("receiver_address")
        String receiverAddress) {
}
