package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderPayCallbackRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("pay_no")
        @NotBlank(message = "支付单号不能为空")
        String payNo,
        @JsonProperty("callback_event_id")
        @NotBlank(message = "回调事件ID不能为空")
        String callbackEventId,
        @JsonProperty("paid_amount")
        @NotNull(message = "支付金额不能为空")
        @DecimalMin(value = "0.00", message = "支付金额不能小于0")
        BigDecimal paidAmount,
        @JsonProperty("third_party_trade_no")
        String thirdPartyTradeNo,
        @JsonProperty("pay_channel")
        @NotBlank(message = "支付渠道不能为空")
        String payChannel,
        @JsonProperty("callback_payload_json")
        String callbackPayloadJson) {
}
