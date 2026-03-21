package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRefundCallbackRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("callback_event_id")
        @NotBlank(message = "回调事件ID不能为空")
        String callbackEventId,
        @JsonProperty("callback_status")
        @NotBlank(message = "回调状态不能为空")
        String callbackStatus,
        @JsonProperty("third_party_refund_no")
        String thirdPartyRefundNo,
        @JsonProperty("failure_reason")
        String failureReason,
        @JsonProperty("callback_payload_json")
        String callbackPayloadJson) {
}
