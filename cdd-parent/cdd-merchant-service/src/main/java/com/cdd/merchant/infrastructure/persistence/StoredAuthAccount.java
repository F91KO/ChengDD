package com.cdd.merchant.infrastructure.persistence;

public record StoredAuthAccount(
        long accountId,
        String userId,
        String accountName,
        String displayName,
        String mobile,
        String merchantId,
        String storeId,
        String miniProgramId,
        String passwordHash,
        String status,
        long operatorId) {
}
