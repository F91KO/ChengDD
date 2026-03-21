package com.cdd.merchant.infrastructure.persistence;

import java.time.Instant;

public record StoredOnboardingTask(
        long id,
        String taskNo,
        long applicationId,
        long merchantId,
        long storeId,
        long miniProgramId,
        String taskStatus,
        String stepCode,
        String validationResultJson,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt) {
}
