package com.cdd.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cdd.api.merchant.model.MerchantApplicationReviewRequest;
import com.cdd.api.merchant.model.MerchantOnboardingActivateRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.merchant.error.MerchantErrorCode;
import com.cdd.merchant.infrastructure.persistence.MerchantOnboardingRepository;
import com.cdd.merchant.infrastructure.persistence.StoredMerchantApplication;
import com.cdd.merchant.infrastructure.persistence.StoredOnboardingTask;
import com.cdd.merchant.support.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MerchantOnboardingApplicationServiceTest {

    private StubRepository repository;

    private MerchantOnboardingApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new StubRepository();
        service = new MerchantOnboardingApplicationService(
                repository,
                new MerchantMiniProgramValidator(),
                new SequenceIdGenerator(),
                new ObjectMapper());
    }

    @Test
    void shouldRejectReviewWhenRejectReasonMissing() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.reviewApplication(1L, new MerchantApplicationReviewRequest("reject", " ")));
        assertEquals(MerchantErrorCode.REJECT_REASON_REQUIRED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldRejectActivateWhenApplicationNotApproved() {
        repository.applications.put(1L, new StoredMerchantApplication(
                1L,
                "测试商家",
                "enterprise",
                "张三",
                "13800138000",
                "李四",
                "生鲜",
                "测试品牌",
                "https://example.com/license.png",
                "submitted",
                null,
                null
        ));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.activate(new MerchantOnboardingActivateRequest(
                        1L,
                        "测试店铺",
                        "wx1234567890abcdef",
                        "abcdefghijklmnop123456",
                        "123456789012",
                        null,
                        "https://example.com")));
        assertEquals(MerchantErrorCode.APPLICATION_STATUS_INVALID.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldRejectSubmitWhenStatusTransitionNotAllowed() {
        repository.submitAllowed = false;
        BusinessException ex = assertThrows(BusinessException.class, () -> service.submitApplication(1L));
        assertEquals(MerchantErrorCode.APPLICATION_STATUS_INVALID.getCode(), ex.getErrorCode().getCode());
    }

    private static final class SequenceIdGenerator implements IdGenerator {
        private long value = 1000L;

        @Override
        public long nextId() {
            value += 1;
            return value;
        }
    }

    private static final class StubRepository implements MerchantOnboardingRepository {
        private final Map<Long, StoredMerchantApplication> applications = new HashMap<>();
        private boolean submitAllowed = true;

        @Override
        public void createApplication(StoredMerchantApplication application) {
            applications.put(application.applicationId(), application);
        }

        @Override
        public Optional<StoredMerchantApplication> findApplicationById(long applicationId) {
            return Optional.ofNullable(applications.get(applicationId));
        }

        @Override
        public boolean submitApplication(long applicationId, Instant submittedAt) {
            return submitAllowed;
        }

        @Override
        public boolean approveApplication(long applicationId) {
            return true;
        }

        @Override
        public boolean rejectApplication(long applicationId, String rejectReason) {
            return true;
        }

        @Override
        public Optional<Long> findMerchantProfileIdByApplicationId(long applicationId) {
            return Optional.empty();
        }

        @Override
        public void createMerchantProfile(long profileId,
                                          String merchantNo,
                                          StoredMerchantApplication application,
                                          Instant settledAt) {
            // no-op for unit tests
        }

        @Override
        public void createStore(long storeId,
                                long merchantId,
                                String storeNo,
                                String storeName) {
            // no-op for unit tests
        }

        @Override
        public void createMiniProgram(long miniProgramId,
                                      long merchantId,
                                      long storeId,
                                      String appId,
                                      String appSecretMasked,
                                      String paymentMchId,
                                      String serverDomain,
                                      String detectResultJson) {
            // no-op for unit tests
        }

        @Override
        public void createOnboardingTask(StoredOnboardingTask task) {
            // no-op for unit tests
        }

        @Override
        public void markOnboardingTaskFailed(String taskNo,
                                             String stepCode,
                                             String errorMessage,
                                             Instant finishedAt) {
            // no-op for unit tests
        }

        @Override
        public void markOnboardingTaskSuccess(String taskNo,
                                              String stepCode,
                                              long merchantId,
                                              long storeId,
                                              long miniProgramId,
                                              Instant finishedAt) {
            // no-op for unit tests
        }

        @Override
        public void activateProfileAndStore(long merchantId,
                                            long storeId) {
            // no-op for unit tests
        }
    }
}
