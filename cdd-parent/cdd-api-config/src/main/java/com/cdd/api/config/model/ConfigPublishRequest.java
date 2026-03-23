package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ConfigPublishRequest(
        @JsonProperty("merchant_id")
        @NotBlank(message = "商家标识不能为空")
        String merchantId,
        @JsonProperty("store_id")
        @NotBlank(message = "店铺标识不能为空")
        String storeId,
        @JsonProperty("operator_name")
        @NotBlank(message = "操作人不能为空")
        String operatorName,
        @JsonProperty("publish_note")
        @NotBlank(message = "发布说明不能为空")
        String publishNote) {
}
