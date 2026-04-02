package com.cdd.merchant.infrastructure.persistence;

import com.cdd.common.core.page.PageQuery;
import com.cdd.common.core.page.PageResult;
import java.util.List;
import java.util.Optional;

public interface MerchantAccountRepository {

    PageResult<StoredMerchantSubAccount> pageSubAccounts(long merchantId, PageQuery pageQuery);

    Optional<StoredMerchantSubAccount> findSubAccount(long merchantId, long accountId);

    void createSubAccount(StoredMerchantSubAccount subAccount);

    void updateSubAccount(StoredMerchantSubAccount subAccount);

    void disableSubAccount(long merchantId, long accountId, long operatorId);

    void replaceScopes(long merchantId, long accountId, String scopeType, List<Long> scopeObjectIds, long operatorId);

    boolean allStoresExist(long merchantId, List<Long> storeIds);

    boolean allMiniProgramsExist(long merchantId, List<Long> miniProgramIds);

    long requireRoleId(String roleCode);

    void createAuthAccount(StoredAuthAccount authAccount);

    void updateAuthAccount(StoredAuthAccount authAccount);

    void createAuthAccountRole(long bindingId, long accountId, long roleId, long operatorId);

    void updateAuthPassword(long accountId, String userId, String passwordHash);

    void disableAuthAccount(long accountId, String userId, long operatorId);
}
