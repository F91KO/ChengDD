package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantMiniProgramValidationRequest(
        @JsonProperty("app_id")
        @NotBlank(message = "小程序AppID不能为空")
        @Size(max = 64, message = "小程序AppID长度不能超过64")
        String appId,
        @JsonProperty("app_secret")
        @NotBlank(message = "小程序AppSecret不能为空")
        @Size(max = 128, message = "小程序AppSecret长度不能超过128")
        String appSecret,
        @JsonProperty("payment_mch_id")
        @Size(max = 64, message = "支付商户号长度不能超过64")
        String paymentMchId,
        @JsonProperty("server_domain")
        @Size(max = 256, message = "服务域名长度不能超过256")
        String serverDomain) {
}
