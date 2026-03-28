package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderShipRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("user_id")
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @JsonProperty("logistics_company_code")
        @NotBlank(message = "物流公司编码不能为空")
        String logisticsCompanyCode,
        @JsonProperty("logistics_company_name")
        @NotBlank(message = "物流公司名称不能为空")
        String logisticsCompanyName,
        @JsonProperty("tracking_no")
        @NotBlank(message = "物流单号不能为空")
        String trackingNo,
        @JsonProperty("ship_remark")
        String shipRemark) {
}
