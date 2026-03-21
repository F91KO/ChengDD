package com.cdd.merchant.web;

import com.cdd.api.merchant.model.MerchantApplicationCreateRequest;
import com.cdd.api.merchant.model.MerchantApplicationResponse;
import com.cdd.api.merchant.model.MerchantApplicationReviewRequest;
import com.cdd.api.merchant.model.MerchantMiniProgramValidationRequest;
import com.cdd.api.merchant.model.MerchantMiniProgramValidationResponse;
import com.cdd.api.merchant.model.MerchantOnboardingActivateRequest;
import com.cdd.api.merchant.model.MerchantOnboardingTaskResponse;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.merchant.service.MerchantOnboardingApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/onboarding")
public class MerchantOnboardingController {

    private final MerchantOnboardingApplicationService onboardingService;

    public MerchantOnboardingController(MerchantOnboardingApplicationService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/applications")
    public ApiResponse<MerchantApplicationResponse> createApplication(
            @Valid @RequestBody MerchantApplicationCreateRequest request) {
        return ApiResponses.success(onboardingService.createApplication(request));
    }

    @GetMapping("/applications/{applicationId}")
    public ApiResponse<MerchantApplicationResponse> getApplication(@PathVariable("applicationId") long applicationId) {
        return ApiResponses.success(onboardingService.getApplication(applicationId));
    }

    @PostMapping("/applications/{applicationId}/submit")
    public ApiResponse<MerchantApplicationResponse> submitApplication(@PathVariable("applicationId") long applicationId) {
        return ApiResponses.success(onboardingService.submitApplication(applicationId));
    }

    @PostMapping("/applications/{applicationId}/review")
    public ApiResponse<MerchantApplicationResponse> reviewApplication(
            @PathVariable("applicationId") long applicationId,
            @Valid @RequestBody MerchantApplicationReviewRequest request) {
        return ApiResponses.success(onboardingService.reviewApplication(applicationId, request));
    }

    @PostMapping("/mini-program/validate")
    public ApiResponse<MerchantMiniProgramValidationResponse> validateMiniProgram(
            @Valid @RequestBody MerchantMiniProgramValidationRequest request) {
        return ApiResponses.success(onboardingService.validateMiniProgram(request));
    }

    @PostMapping("/activate")
    public ApiResponse<MerchantOnboardingTaskResponse> activate(
            @Valid @RequestBody MerchantOnboardingActivateRequest request) {
        return ApiResponses.success(onboardingService.activate(request));
    }
}
