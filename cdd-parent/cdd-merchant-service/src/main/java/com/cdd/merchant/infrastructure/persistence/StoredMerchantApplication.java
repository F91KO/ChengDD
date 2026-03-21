package com.cdd.merchant.infrastructure.persistence;

import java.time.Instant;

public record StoredMerchantApplication(
        long applicationId,
        String merchantName,
        String merchantType,
        String contactName,
        String contactMobile,
        String legalPersonName,
        String businessCategory,
        String brandName,
        String licenseFileUrl,
        String status,
        String rejectReason,
        Instant submittedAt) {
}
