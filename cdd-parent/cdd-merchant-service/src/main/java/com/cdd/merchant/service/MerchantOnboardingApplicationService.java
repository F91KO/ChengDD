package com.cdd.merchant.service;

import com.cdd.api.merchant.model.MerchantApplicationCreateRequest;
import com.cdd.api.merchant.model.MerchantApplicationResponse;
import com.cdd.api.merchant.model.MerchantApplicationReviewRequest;
import com.cdd.api.merchant.model.MerchantMiniProgramValidationRequest;
import com.cdd.api.merchant.model.MerchantMiniProgramValidationResponse;
import com.cdd.api.merchant.model.MerchantOnboardingActivateRequest;
import com.cdd.api.merchant.model.MerchantOnboardingTaskResponse;
import com.cdd.common.core.error.BusinessException;
import com.cdd.merchant.error.MerchantErrorCode;
import com.cdd.merchant.infrastructure.persistence.MerchantOnboardingRepository;
import com.cdd.merchant.infrastructure.persistence.StoredMerchantApplication;
import com.cdd.merchant.infrastructure.persistence.StoredOnboardingTask;
import com.cdd.merchant.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MerchantOnboardingApplicationService {

    private static final String REVIEW_APPROVE = "approve";
    private static final String REVIEW_REJECT = "reject";

    private final MerchantOnboardingRepository repository;
    private final MerchantMiniProgramValidator validator;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public MerchantOnboardingApplicationService(MerchantOnboardingRepository repository,
                                                MerchantMiniProgramValidator validator,
                                                IdGenerator idGenerator,
                                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.validator = validator;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MerchantApplicationResponse createApplication(MerchantApplicationCreateRequest request) {
        StoredMerchantApplication application = new StoredMerchantApplication(
                idGenerator.nextId(),
                request.merchantName().trim(),
                normalize(request.merchantType()),
                request.contactName().trim(),
                request.contactMobile().trim(),
                request.legalPersonName().trim(),
                request.businessCategory().trim(),
                trimToNull(request.brandName()),
                request.licenseFileUrl().trim(),
                "draft",
                null,
                null);
        repository.createApplication(application);
        return toResponse(application);
    }

    @Transactional
    public MerchantApplicationResponse submitApplication(long applicationId) {
        boolean updated = repository.submitApplication(applicationId, Instant.now());
        if (!updated) {
            throw new BusinessException(MerchantErrorCode.APPLICATION_STATUS_INVALID, "当前申请状态不允许提交");
        }
        return toResponse(requireApplication(applicationId));
    }

    @Transactional
    public MerchantApplicationResponse reviewApplication(long applicationId, MerchantApplicationReviewRequest request) {
        String decision = normalize(request.decision());
        boolean updated;
        if (REVIEW_APPROVE.equals(decision)) {
            updated = repository.approveApplication(applicationId);
        } else if (REVIEW_REJECT.equals(decision)) {
            if (!StringUtils.hasText(request.rejectReason())) {
                throw new BusinessException(MerchantErrorCode.REJECT_REASON_REQUIRED);
            }
            updated = repository.rejectApplication(applicationId, request.rejectReason().trim());
        } else {
            throw new BusinessException(MerchantErrorCode.REVIEW_DECISION_INVALID);
        }
        if (!updated) {
            throw new BusinessException(MerchantErrorCode.APPLICATION_STATUS_INVALID, "当前申请状态不允许审核");
        }
        return toResponse(requireApplication(applicationId));
    }

    public MerchantApplicationResponse getApplication(long applicationId) {
        return toResponse(requireApplication(applicationId));
    }

    public MerchantMiniProgramValidationResponse validateMiniProgram(MerchantMiniProgramValidationRequest request) {
        MerchantMiniProgramValidationResult result = validator.validate(
                request.appId(),
                request.appSecret(),
                request.paymentMchId(),
                request.serverDomain());
        return new MerchantMiniProgramValidationResponse(result.passed(), result.issues());
    }

    @Transactional
    public MerchantOnboardingTaskResponse activate(MerchantOnboardingActivateRequest request) {
        StoredMerchantApplication application = requireApplication(request.applicationId());
        if (!"approved".equals(normalize(application.status()))) {
            throw new BusinessException(MerchantErrorCode.APPLICATION_STATUS_INVALID, "仅已审核通过的申请可执行一键开通");
        }
        String taskNo = "onb_" + idGenerator.nextId();
        long taskId = idGenerator.nextId();
        Instant startedAt = Instant.now();
        StoredOnboardingTask task = new StoredOnboardingTask(
                taskId,
                taskNo,
                application.applicationId(),
                0L,
                0L,
                0L,
                "running",
                "validate_mini_program",
                null,
                null,
                startedAt,
                null);
        repository.createOnboardingTask(task);
        MerchantMiniProgramValidationResult validationResult = validator.validate(
                request.appId(),
                request.appSecret(),
                request.paymentMchId(),
                request.serverDomain());
        if (!validationResult.passed()) {
            String message = String.join("；", validationResult.issues());
            repository.markOnboardingTaskFailed(taskNo, "validate_mini_program", message, Instant.now());
            throw new BusinessException(MerchantErrorCode.MINI_PROGRAM_VALIDATION_FAILED, message);
        }
        long merchantId;
        long storeId;
        long miniProgramId;
        try {
            merchantId = repository.findMerchantProfileIdByApplicationId(application.applicationId())
                    .orElseGet(() -> {
                        long profileId = idGenerator.nextId();
                        repository.createMerchantProfile(profileId, "M" + profileId, application, Instant.now());
                        return profileId;
                    });
            storeId = idGenerator.nextId();
            repository.createStore(storeId, merchantId, "S" + storeId, request.storeName().trim());
            miniProgramId = idGenerator.nextId();
            String validationJson = toValidationJson(validationResult);
            repository.createMiniProgram(
                    miniProgramId,
                    merchantId,
                    storeId,
                    request.appId().trim(),
                    maskSecret(request.appSecret()),
                    trimToNull(request.paymentMchId()),
                    trimToNull(request.serverDomain()),
                    validationJson);
            repository.activateProfileAndStore(merchantId, storeId);
            repository.markOnboardingTaskSuccess(taskNo, "onboarding_finished", merchantId, storeId, miniProgramId, Instant.now());
        } catch (DataIntegrityViolationException ex) {
            repository.markOnboardingTaskFailed(taskNo, "create_mini_program", "小程序AppID已存在", Instant.now());
            throw new BusinessException(MerchantErrorCode.MINI_PROGRAM_APP_ID_CONFLICT);
        } catch (BusinessException ex) {
            repository.markOnboardingTaskFailed(taskNo, "onboarding_failed", ex.getMessage(), Instant.now());
            throw ex;
        } catch (Exception ex) {
            repository.markOnboardingTaskFailed(taskNo, "onboarding_failed", "开通流程执行异常", Instant.now());
            throw new BusinessException(MerchantErrorCode.ONBOARDING_FAILED);
        }
        return new MerchantOnboardingTaskResponse(
                taskNo,
                "success",
                "onboarding_finished",
                merchantId,
                storeId,
                miniProgramId,
                request.categoryTemplateId(),
                null,
                "一键开通执行完成");
    }

    private StoredMerchantApplication requireApplication(long applicationId) {
        Optional<StoredMerchantApplication> application = repository.findApplicationById(applicationId);
        return application.orElseThrow(() -> new BusinessException(MerchantErrorCode.APPLICATION_NOT_FOUND));
    }

    private MerchantApplicationResponse toResponse(StoredMerchantApplication application) {
        return new MerchantApplicationResponse(
                application.applicationId(),
                application.merchantName(),
                application.merchantType(),
                application.contactName(),
                application.contactMobile(),
                application.status(),
                application.rejectReason(),
                application.submittedAt());
    }

    private String toValidationJson(MerchantMiniProgramValidationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            return "{\"passed\":true,\"issues\":[]}";
        }
    }

    private static String maskSecret(String appSecret) {
        String value = appSecret == null ? "" : appSecret.trim();
        if (value.length() <= 6) {
            return "******";
        }
        return value.substring(0, 3) + "******" + value.substring(value.length() - 3);
    }

    private static String trimToNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
