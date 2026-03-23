package com.cdd.api.decoration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record StoreDecorationConfigResponse(
        @JsonProperty("config_id")
        Long configId,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("mini_program_id")
        Long miniProgramId,
        @JsonProperty("theme_color")
        String themeColor,
        @JsonProperty("home_template_code")
        String homeTemplateCode,
        @JsonProperty("home_style_mode")
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
        @JsonProperty("status")
        String status,
        @JsonProperty("version_no")
        Integer versionNo,
        @JsonProperty("home_modules")
        List<DecorationHomeModuleResponse> homeModules) {
}
