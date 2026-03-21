package com.cdd.merchant.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

public interface MerchantOnboardingRepository {

    void createApplication(StoredMerchantApplication application);

    Optional<StoredMerchantApplication> findApplicationById(long applicationId);

    boolean submitApplication(long applicationId, Instant submittedAt);

    boolean approveApplication(long applicationId);

    boolean rejectApplication(long applicationId, String rejectReason);

    Optional<Long> findMerchantProfileIdByApplicationId(long applicationId);

    void createMerchantProfile(long profileId,
                               String merchantNo,
                               StoredMerchantApplication application,
                               Instant settledAt);

    void createStore(long storeId,
                     long merchantId,
                     String storeNo,
                     String storeName);

    void createMiniProgram(long miniProgramId,
                           long merchantId,
                           long storeId,
                           String appId,
                           String appSecretMasked,
                           String paymentMchId,
                           String serverDomain,
                           String detectResultJson);

    void createOnboardingTask(StoredOnboardingTask task);

    void markOnboardingTaskFailed(String taskNo,
                                  String stepCode,
                                  String errorMessage,
                                  Instant finishedAt);

    void markOnboardingTaskSuccess(String taskNo,
                                   String stepCode,
                                   long merchantId,
                                   long storeId,
                                   long miniProgramId,
                                   Instant finishedAt);

    void activateProfileAndStore(long merchantId,
                                 long storeId);
}
