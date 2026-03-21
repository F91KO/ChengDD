package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantApplicationCreateRequest(
        @JsonProperty("merchant_name")
        @NotBlank(message = "商家主体名称不能为空")
        @Size(max = 128, message = "商家主体名称长度不能超过128")
        String merchantName,
        @JsonProperty("merchant_type")
        @NotBlank(message = "商家类型不能为空")
        @Size(max = 32, message = "商家类型长度不能超过32")
        String merchantType,
        @JsonProperty("contact_name")
        @NotBlank(message = "联系人不能为空")
        @Size(max = 64, message = "联系人长度不能超过64")
        String contactName,
        @JsonProperty("contact_mobile")
        @NotBlank(message = "联系电话不能为空")
        @Size(max = 32, message = "联系电话长度不能超过32")
        String contactMobile,
        @JsonProperty("legal_person_name")
        @NotBlank(message = "法人姓名不能为空")
        @Size(max = 64, message = "法人姓名长度不能超过64")
        String legalPersonName,
        @JsonProperty("business_category")
        @NotBlank(message = "经营类目不能为空")
        @Size(max = 64, message = "经营类目长度不能超过64")
        String businessCategory,
        @JsonProperty("brand_name")
        @Size(max = 128, message = "品牌名称长度不能超过128")
        String brandName,
        @JsonProperty("license_file_url")
        @NotBlank(message = "营业执照地址不能为空")
        @Size(max = 512, message = "营业执照地址长度不能超过512")
        String licenseFileUrl) {
}
