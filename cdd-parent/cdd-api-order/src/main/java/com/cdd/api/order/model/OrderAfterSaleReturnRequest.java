package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderAfterSaleReturnRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("user_id")
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @JsonProperty("return_company")
        @NotBlank(message = "退货物流公司不能为空")
        String returnCompany,
        @JsonProperty("return_logistics_no")
        @NotBlank(message = "退货物流单号不能为空")
        String returnLogisticsNo) {
}
