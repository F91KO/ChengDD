package com.cdd.merchant.service;

import com.cdd.api.merchant.model.MerchantSubAccountResetLoginResponse;
import com.cdd.api.merchant.model.MerchantSubAccountResponse;
import com.cdd.api.merchant.model.MerchantSubAccountUpsertRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.page.PageQuery;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import com.cdd.common.web.PageResponse;
import com.cdd.merchant.error.MerchantErrorCode;
import com.cdd.merchant.infrastructure.persistence.JdbcMerchantAccountRepository;
import com.cdd.merchant.infrastructure.persistence.StoredAuthAccount;
import com.cdd.merchant.infrastructure.persistence.StoredMerchantSubAccount;
import com.cdd.merchant.support.IdGenerator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MerchantAccountApplicationService {

    private static final String MERCHANT_ADMIN_ROLE_CODE = "merchant_admin";

    private final JdbcMerchantAccountRepository repository;
    private final IdGenerator idGenerator;

    public MerchantAccountApplicationService(JdbcMerchantAccountRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    public PageResponse<MerchantSubAccountResponse> pageSubAccounts(PageQuery pageQuery) {
        long merchantId = requiredMerchantNumericId();
        var pageResult = repository.pageSubAccounts(merchantId, pageQuery);
        List<MerchantSubAccountResponse> list = pageResult.list().stream()
                .map(this::hydrateResponse)
                .toList();
        return PageResponse.of(list, pageQuery.page(), pageQuery.pageSize(), pageResult.total());
    }

    @Transactional
    public MerchantSubAccountResponse createSubAccount(MerchantSubAccountUpsertRequest request) {
        AuthContext authContext = requiredAuthContext();
        long merchantId = requiredMerchantNumericId(authContext);
        long operatorId = parseNumericTail(authContext.getUserId());
        List<String> scopeIds = normalizedScopeIds(request.dataScopeType(), request.dataScopeIds());
        validateScopeIds(merchantId, request.dataScopeType(), scopeIds);

        long accountId = idGenerator.nextId();
        String temporaryPassword = temporaryPassword(accountId);
        StoredMerchantSubAccount subAccount = new StoredMerchantSubAccount(
                accountId,
                merchantId,
                request.accountName().trim(),
                request.displayName().trim(),
                request.mobile().trim(),
                trimToNull(request.remark()),
                "enabled",
                "商家子账号",
                normalizeList(request.permissionModules()),
                normalizeList(request.actionPermissions()),
                normalize(request.dataScopeType()),
                scopeIds);
        repository.createSubAccount(subAccount);
        repository.replaceScopes(merchantId, accountId, normalize(request.dataScopeType()), parseScopeObjectIds(scopeIds), operatorId);
        long roleId = repository.requireRoleId(MERCHANT_ADMIN_ROLE_CODE);
        if (roleId < 0) {
            throw new BusinessException(MerchantErrorCode.SUB_ACCOUNT_ROLE_NOT_FOUND);
        }
        repository.createAuthAccount(new StoredAuthAccount(
                accountId,
                "ms_" + accountId,
                request.accountName().trim(),
                request.displayName().trim(),
                request.mobile().trim(),
                authContext.getMerchantId(),
                resolveStoreId(scopeIds, authContext),
                resolveMiniProgramId(scopeIds, authContext),
                "{noop}" + temporaryPassword,
                "enabled",
                operatorId));
        repository.createAuthAccountRole(idGenerator.nextId(), accountId, roleId, operatorId);
        return hydrateResponse(subAccount);
    }

    @Transactional
    public MerchantSubAccountResponse updateSubAccount(long accountId, MerchantSubAccountUpsertRequest request) {
        AuthContext authContext = requiredAuthContext();
        long merchantId = requiredMerchantNumericId(authContext);
        long operatorId = parseNumericTail(authContext.getUserId());
        requireStoredAccount(merchantId, accountId);
        List<String> scopeIds = normalizedScopeIds(request.dataScopeType(), request.dataScopeIds());
        validateScopeIds(merchantId, request.dataScopeType(), scopeIds);
        StoredMerchantSubAccount subAccount = new StoredMerchantSubAccount(
                accountId,
                merchantId,
                request.accountName().trim(),
                request.displayName().trim(),
                request.mobile().trim(),
                trimToNull(request.remark()),
                "enabled",
                "商家子账号",
                normalizeList(request.permissionModules()),
                normalizeList(request.actionPermissions()),
                normalize(request.dataScopeType()),
                scopeIds);
        repository.updateSubAccount(subAccount);
        repository.replaceScopes(merchantId, accountId, normalize(request.dataScopeType()), parseScopeObjectIds(scopeIds), operatorId);
        repository.updateAuthAccount(new StoredAuthAccount(
                accountId,
                "ms_" + accountId,
                request.accountName().trim(),
                request.displayName().trim(),
                request.mobile().trim(),
                authContext.getMerchantId(),
                resolveStoreId(scopeIds, authContext),
                resolveMiniProgramId(scopeIds, authContext),
                null,
                "enabled",
                operatorId));
        return hydrateResponse(subAccount);
    }

    @Transactional
    public MerchantSubAccountResponse disableSubAccount(long accountId) {
        AuthContext authContext = requiredAuthContext();
        long merchantId = requiredMerchantNumericId(authContext);
        long operatorId = parseNumericTail(authContext.getUserId());
        StoredMerchantSubAccount existing = requireStoredAccount(merchantId, accountId);
        repository.disableSubAccount(merchantId, accountId, operatorId);
        repository.disableAuthAccount(accountId, "ms_" + accountId, operatorId);
        return hydrateResponse(new StoredMerchantSubAccount(
                existing.accountId(),
                existing.merchantId(),
                existing.accountName(),
                existing.displayName(),
                existing.mobile(),
                existing.remark(),
                "disabled",
                existing.roleLabel(),
                existing.permissionModules(),
                existing.actionPermissions(),
                existing.dataScopeType(),
                existing.dataScopeIds()));
    }

    @Transactional
    public MerchantSubAccountResetLoginResponse resetLogin(long accountId) {
        AuthContext authContext = requiredAuthContext();
        long merchantId = requiredMerchantNumericId(authContext);
        requireStoredAccount(merchantId, accountId);
        String temporaryPassword = temporaryPassword(accountId + 7);
        repository.updateAuthPassword(accountId, "ms_" + accountId, "{noop}" + temporaryPassword);
        return new MerchantSubAccountResetLoginResponse(String.valueOf(accountId), temporaryPassword);
    }

    private MerchantSubAccountResponse hydrateResponse(StoredMerchantSubAccount subAccount) {
        List<String> permissionModules = subAccount.permissionModules().isEmpty()
                ? repository.loadPermissionModules(subAccount.accountId())
                : subAccount.permissionModules();
        List<String> actionPermissions = subAccount.actionPermissions().isEmpty()
                ? repository.loadActionPermissions(subAccount.accountId())
                : subAccount.actionPermissions();
        List<String> scopeIds = subAccount.dataScopeIds().isEmpty()
                ? repository.loadDataScopeIds(subAccount.accountId(), subAccount.dataScopeType())
                : subAccount.dataScopeIds();
        return new MerchantSubAccountResponse(
                String.valueOf(subAccount.accountId()),
                subAccount.accountName(),
                subAccount.displayName(),
                subAccount.mobile(),
                subAccount.remark(),
                subAccount.status(),
                subAccount.roleLabel(),
                permissionModules,
                actionPermissions,
                subAccount.dataScopeType(),
                scopeIds);
    }

    private void validateScopeIds(long merchantId, String dataScopeType, List<String> dataScopeIds) {
        String scopeType = normalize(dataScopeType);
        if ("merchant".equals(scopeType)) {
            return;
        }
        if ("store".equals(scopeType)) {
            if (!repository.allStoresExist(merchantId, parseScopeObjectIds(dataScopeIds))) {
                throw new BusinessException(MerchantErrorCode.SUB_ACCOUNT_SCOPE_INVALID);
            }
            return;
        }
        if ("mini_program".equals(scopeType)) {
            if (!repository.allMiniProgramsExist(merchantId, parseScopeObjectIds(dataScopeIds))) {
                throw new BusinessException(MerchantErrorCode.SUB_ACCOUNT_SCOPE_INVALID);
            }
            return;
        }
        throw new BusinessException(MerchantErrorCode.SUB_ACCOUNT_SCOPE_TYPE_INVALID);
    }

    private List<Long> parseScopeObjectIds(List<String> dataScopeIds) {
        return dataScopeIds.stream()
                .map(MerchantAccountApplicationService::parseNumericTail)
                .filter(value -> value > 0)
                .toList();
    }

    private String resolveStoreId(List<String> dataScopeIds, AuthContext authContext) {
        return dataScopeIds.stream()
                .filter(value -> value.startsWith("store_"))
                .findFirst()
                .orElse(authContext.getStoreId());
    }

    private String resolveMiniProgramId(List<String> dataScopeIds, AuthContext authContext) {
        return dataScopeIds.stream()
                .filter(value -> value.startsWith("mini_program_"))
                .findFirst()
                .orElse(authContext.getMiniProgramId());
    }

    private StoredMerchantSubAccount requireStoredAccount(long merchantId, long accountId) {
        return repository.findSubAccount(merchantId, accountId)
                .orElseThrow(() -> new BusinessException(MerchantErrorCode.SUB_ACCOUNT_NOT_FOUND));
    }

    private long requiredMerchantNumericId() {
        return requiredMerchantNumericId(requiredAuthContext());
    }

    private long requiredMerchantNumericId(AuthContext authContext) {
        long merchantId = parseNumericTail(authContext.getMerchantId());
        if (merchantId <= 0) {
            throw new BusinessException(MerchantErrorCode.SUB_ACCOUNT_SCOPE_INVALID, "当前账号缺少商家范围");
        }
        return merchantId;
    }

    private AuthContext requiredAuthContext() {
        AuthContext authContext = AuthContextHolder.get();
        if (authContext == null) {
            throw new BusinessException(MerchantErrorCode.SUB_ACCOUNT_SCOPE_INVALID, "当前账号未登录");
        }
        return authContext;
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static List<String> normalizedScopeIds(String dataScopeType, List<String> values) {
        String scopeType = normalize(dataScopeType);
        if ("merchant".equals(scopeType) || values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> value.contains("_") ? value : scopeType + "_" + value)
                .distinct()
                .toList();
    }

    private static String temporaryPassword(long seed) {
        String suffix = String.valueOf(Math.abs(seed));
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return "Temp@" + suffix;
    }

    private static long parseNumericTail(String raw) {
        if (!StringUtils.hasText(raw)) {
            return -1L;
        }
        String trimmed = raw.trim();
        int separatorIndex = trimmed.lastIndexOf('_');
        String numericPart = separatorIndex >= 0 ? trimmed.substring(separatorIndex + 1) : trimmed;
        try {
            return Long.parseLong(numericPart);
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
