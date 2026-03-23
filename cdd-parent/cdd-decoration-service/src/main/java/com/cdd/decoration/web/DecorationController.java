package com.cdd.decoration.web;

import com.cdd.api.decoration.model.PublishDecorationRequest;
import com.cdd.api.decoration.model.RollbackDecorationRequest;
import com.cdd.api.decoration.model.SaveDecorationDraftRequest;
import com.cdd.api.decoration.model.StoreDecorationConfigResponse;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.decoration.service.DecorationApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/decoration")
public class DecorationController {

    private final DecorationApplicationService decorationApplicationService;

    public DecorationController(DecorationApplicationService decorationApplicationService) {
        this.decorationApplicationService = decorationApplicationService;
    }

    @GetMapping("/merchant/stores/{store_id}/decoration")
    public ApiResponse<StoreDecorationConfigResponse> getDecorationConfig(
            @PathVariable("store_id") Long storeId,
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("mini_program_id") @NotNull(message = "小程序ID不能为空") Long miniProgramId) {
        return ApiResponses.success(decorationApplicationService.getDecorationConfig(merchantId, storeId, miniProgramId));
    }

    @PostMapping("/merchant/stores/{store_id}/decoration/draft")
    public ApiResponse<StoreDecorationConfigResponse> saveDraft(
            @PathVariable("store_id") Long storeId,
            @Valid @RequestBody SaveDecorationDraftRequest request) {
        return ApiResponses.success(decorationApplicationService.saveDraft(storeId, request));
    }

    @PostMapping("/merchant/stores/{store_id}/decoration/publish")
    public ApiResponse<StoreDecorationConfigResponse> publish(
            @PathVariable("store_id") Long storeId,
            @Valid @RequestBody PublishDecorationRequest request) {
        return ApiResponses.success(decorationApplicationService.publish(storeId, request));
    }

    @PostMapping("/merchant/stores/{store_id}/decoration/rollback")
    public ApiResponse<StoreDecorationConfigResponse> rollback(
            @PathVariable("store_id") Long storeId,
            @Valid @RequestBody RollbackDecorationRequest request) {
        return ApiResponses.success(decorationApplicationService.rollback(storeId, request));
    }

    @PostMapping("/merchant/stores/{store_id}/decoration/preview")
    public ApiResponse<StoreDecorationConfigResponse> preview(
            @PathVariable("store_id") Long storeId,
            @Valid @RequestBody SaveDecorationDraftRequest request) {
        return ApiResponses.success(decorationApplicationService.preview(storeId, request));
    }
}
