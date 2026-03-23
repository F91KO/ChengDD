package com.cdd.config.web;

import com.cdd.api.config.model.ConfigPublishRecordResponse;
import com.cdd.api.config.model.ConfigPublishRequest;
import com.cdd.api.config.model.ConfigPublishRollbackRequest;
import com.cdd.api.config.model.ConfigKvUpsertRequest;
import com.cdd.api.config.model.ConfigKvValueResponse;
import com.cdd.api.config.model.FeatureSwitchStatusChangeRequest;
import com.cdd.api.config.model.FeatureSwitchUpsertRequest;
import com.cdd.api.config.model.FeatureSwitchValueResponse;
import com.cdd.api.config.model.MerchantConfigKvOverrideRequest;
import com.cdd.api.config.model.MerchantFeatureSwitchChangeRequest;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.config.service.ConfigApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigApplicationService configApplicationService;

    public ConfigController(ConfigApplicationService configApplicationService) {
        this.configApplicationService = configApplicationService;
    }

    @PostMapping("/platform/kv")
    public ApiResponse<ConfigKvValueResponse> upsertPlatformConfig(
            @Valid @RequestBody ConfigKvUpsertRequest request) {
        return ApiResponses.success(configApplicationService.upsertPlatformConfig(request));
    }

    @PostMapping("/platform/kv/merchant-overrides")
    public ApiResponse<ConfigKvValueResponse> upsertMerchantConfigOverride(
            @Valid @RequestBody MerchantConfigKvOverrideRequest request) {
        return ApiResponses.success(configApplicationService.upsertMerchantConfigOverride(request));
    }

    @GetMapping("/platform/kv/effective")
    public ApiResponse<ConfigKvValueResponse> getEffectiveConfig(
            @RequestParam(value = "merchant_id", required = false) String merchantId,
            @RequestParam("config_group") @NotBlank(message = "配置分组不能为空") String configGroup,
            @RequestParam("config_key") @NotBlank(message = "配置键不能为空") String configKey) {
        return ApiResponses.success(configApplicationService.getEffectiveConfig(merchantId, configGroup, configKey));
    }

    @PostMapping("/platform/feature-switches")
    public ApiResponse<FeatureSwitchValueResponse> upsertFeatureSwitch(
            @Valid @RequestBody FeatureSwitchUpsertRequest request) {
        return ApiResponses.success(configApplicationService.upsertFeatureSwitch(request));
    }

    @PostMapping("/platform/feature-switches/{switchCode}/change-status")
    public ApiResponse<FeatureSwitchValueResponse> changeFeatureSwitchStatus(
            @PathVariable("switchCode") String switchCode,
            @Valid @RequestBody FeatureSwitchStatusChangeRequest request) {
        return ApiResponses.success(configApplicationService.changeFeatureSwitchStatus(switchCode, request));
    }

    @GetMapping("/platform/feature-switches")
    public ApiResponse<List<FeatureSwitchValueResponse>> listPlatformFeatureSwitches() {
        return ApiResponses.success(configApplicationService.listPlatformFeatureSwitches());
    }

    @GetMapping("/platform/feature-switches/{switchCode}")
    public ApiResponse<FeatureSwitchValueResponse> getPlatformFeatureSwitch(
            @PathVariable("switchCode") String switchCode) {
        return ApiResponses.success(configApplicationService.getPlatformFeatureSwitch(switchCode));
    }

    @PostMapping("/merchant/feature-switches/{switchCode}/change")
    public ApiResponse<FeatureSwitchValueResponse> changeMerchantFeatureSwitch(
            @PathVariable("switchCode") String switchCode,
            @Valid @RequestBody MerchantFeatureSwitchChangeRequest request) {
        return ApiResponses.success(configApplicationService.changeMerchantFeatureSwitch(switchCode, request));
    }

    @GetMapping("/merchant/feature-switches")
    public ApiResponse<List<FeatureSwitchValueResponse>> listMerchantFeatureSwitches(
            @RequestParam("merchant_id") @NotBlank(message = "商家标识不能为空") String merchantId) {
        return ApiResponses.success(configApplicationService.listMerchantFeatureSwitches(merchantId));
    }

    @GetMapping("/merchant/feature-switches/{switchCode}")
    public ApiResponse<FeatureSwitchValueResponse> getMerchantFeatureSwitch(
            @PathVariable("switchCode") String switchCode,
            @RequestParam("merchant_id") @NotBlank(message = "商家标识不能为空") String merchantId) {
        return ApiResponses.success(configApplicationService.getMerchantFeatureSwitch(switchCode, merchantId));
    }

    @GetMapping("/publish-records")
    public ApiResponse<List<ConfigPublishRecordResponse>> listPublishRecords(
            @RequestParam("merchant_id") @NotBlank(message = "商家标识不能为空") String merchantId,
            @RequestParam(value = "store_id", required = false) String storeId) {
        return ApiResponses.success(configApplicationService.listPublishRecords(merchantId, storeId));
    }

    @PostMapping("/publish-records")
    public ApiResponse<ConfigPublishRecordResponse> createPublishRecord(
            @Valid @RequestBody ConfigPublishRequest request) {
        return ApiResponses.success(configApplicationService.createPublishRecord(request));
    }

    @GetMapping("/publish-records/{task_no}")
    public ApiResponse<ConfigPublishRecordResponse> getPublishRecord(@PathVariable("task_no") String taskNo) {
        return ApiResponses.success(configApplicationService.getPublishRecord(taskNo));
    }

    @PostMapping("/publish-records/{task_no}/rollback")
    public ApiResponse<ConfigPublishRecordResponse> rollbackPublishRecord(
            @PathVariable("task_no") String taskNo,
            @Valid @RequestBody ConfigPublishRollbackRequest request) {
        return ApiResponses.success(configApplicationService.rollbackPublishRecord(taskNo, request));
    }
}
