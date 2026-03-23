package com.cdd.marketing.web;

import com.cdd.api.marketing.model.MarketingActivityResponse;
import com.cdd.api.marketing.model.MarketingActivityUpsertRequest;
import com.cdd.api.marketing.model.MarketingCouponResponse;
import com.cdd.api.marketing.model.MarketingCouponUpsertRequest;
import com.cdd.api.marketing.model.MarketingRecommendRuleResponse;
import com.cdd.api.marketing.model.MarketingRecommendRuleUpsertRequest;
import com.cdd.api.marketing.model.MarketingTopicFloorResponse;
import com.cdd.api.marketing.model.MarketingTopicFloorUpsertRequest;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.marketing.service.MarketingApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/marketing")
public class MarketingController {

    private final MarketingApplicationService marketingApplicationService;

    public MarketingController(MarketingApplicationService marketingApplicationService) {
        this.marketingApplicationService = marketingApplicationService;
    }

    @PostMapping("/coupons")
    public ApiResponse<MarketingCouponResponse> createCoupon(@Valid @RequestBody MarketingCouponUpsertRequest request) {
        return ApiResponses.success(marketingApplicationService.createCoupon(request));
    }

    @GetMapping("/coupons")
    public ApiResponse<List<MarketingCouponResponse>> listCoupons(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponses.success(marketingApplicationService.listCoupons(merchantId, storeId, status));
    }

    @PostMapping("/activities")
    public ApiResponse<MarketingActivityResponse> createActivity(@Valid @RequestBody MarketingActivityUpsertRequest request) {
        return ApiResponses.success(marketingApplicationService.createActivity(request));
    }

    @GetMapping("/activities")
    public ApiResponse<List<MarketingActivityResponse>> listActivities(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponses.success(marketingApplicationService.listActivities(merchantId, storeId, status));
    }

    @PostMapping("/recommend-rules")
    public ApiResponse<MarketingRecommendRuleResponse> createRecommendRule(
            @Valid @RequestBody MarketingRecommendRuleUpsertRequest request) {
        return ApiResponses.success(marketingApplicationService.createRecommendRule(request));
    }

    @GetMapping("/recommend-rules")
    public ApiResponse<List<MarketingRecommendRuleResponse>> listRecommendRules(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "scene_code", required = false) String sceneCode,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponses.success(marketingApplicationService.listRecommendRules(merchantId, storeId, sceneCode, status));
    }

    @PostMapping("/topic-floors")
    public ApiResponse<MarketingTopicFloorResponse> upsertTopicFloor(
            @Valid @RequestBody MarketingTopicFloorUpsertRequest request) {
        return ApiResponses.success(marketingApplicationService.upsertTopicFloor(request));
    }

    @GetMapping("/topic-floors")
    public ApiResponse<List<MarketingTopicFloorResponse>> listTopicFloors(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponses.success(marketingApplicationService.listTopicFloors(merchantId, storeId, status));
    }
}
