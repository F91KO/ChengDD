package com.cdd.merchant.infrastructure.persistence;

import java.util.List;

public record StoredMerchantSubAccount(
        long accountId,
        long merchantId,
        String accountName,
        String displayName,
        String mobile,
        String remark,
        String status,
        String roleLabel,
        List<String> permissionModules,
        List<String> actionPermissions,
        String dataScopeType,
        List<String> dataScopeIds) {
}
