package com.cdd.api.decoration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveDecorationDraftRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("mini_program_id")
        @NotNull(message = "小程序ID不能为空")
        Long miniProgramId,
        @JsonProperty("theme_color")
        @NotBlank(message = "主题色不能为空")
        String themeColor,
        @JsonProperty("home_template_code")
        @NotBlank(message = "首页模板编码不能为空")
        String homeTemplateCode,
        @JsonProperty("home_style_mode")
        @NotBlank(message = "首页风格不能为空")
        String homeStyleMode,
        @JsonProperty("header_service_bar")
        JsonNode headerServiceBar,
        @JsonProperty("brand_hero_block")
        JsonNode brandHeroBlock,
        @JsonProperty("delivery_promise_text")
        String deliveryPromiseText,
        @JsonProperty("delivery_fee_text")
        String deliveryFeeText,
        @JsonProperty("minimum_order_text")
        String minimumOrderText,
        @JsonProperty("search_placeholder")
        String searchPlaceholder,
        @JsonProperty("announcement_text")
        String announcementText,
        @JsonProperty("home_modules")
        @NotEmpty(message = "首页模块不能为空")
        List<@Valid DecorationHomeModuleRequest> homeModules) {
}
