package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderPaySuccessRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("user_id")
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @JsonProperty("pay_no")
        @NotBlank(message = "支付单号不能为空")
        String payNo,
        @JsonProperty("third_party_trade_no")
        String thirdPartyTradeNo,
        @JsonProperty("paid_amount")
        @NotNull(message = "实付金额不能为空")
        @DecimalMin(value = "0.00", message = "实付金额不能小于0")
        BigDecimal paidAmount) {
}
