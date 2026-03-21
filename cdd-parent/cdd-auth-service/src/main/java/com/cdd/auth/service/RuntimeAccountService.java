package com.cdd.auth.service;

import com.cdd.auth.config.AuthServiceProperties;
import com.cdd.auth.error.AuthErrorCode;
import com.cdd.common.core.error.BusinessException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.util.StringUtils;

public class RuntimeAccountService {

    private final Map<String, AccountState> accountsByAccountName = new ConcurrentHashMap<>();
    private final Map<String, AccountState> accountsByUserId = new ConcurrentHashMap<>();

    public RuntimeAccountService(AuthServiceProperties authServiceProperties) {
        for (AuthServiceProperties.BootstrapAccount bootstrapAccount : authServiceProperties.getBootstrapAccounts()) {
            AccountState accountState = AccountState.from(bootstrapAccount);
            AccountState previousByName = accountsByAccountName.putIfAbsent(accountState.accountName(), accountState);
            if (previousByName != null) {
                throw new IllegalStateException("存在重复的账号名配置: " + accountState.accountName());
            }
            AccountState previousByUserId = accountsByUserId.putIfAbsent(accountState.userId(), accountState);
            if (previousByUserId != null) {
                throw new IllegalStateException("存在重复的用户 ID 配置: " + accountState.userId());
            }
        }
    }

    public AuthenticatedAccount authenticate(String expectedAccountType, String accountName, String password) {
        AccountState accountState = accountsByAccountName.get(accountName);
        if (accountState == null || !accountState.matchesPassword(password) || !accountState.matchesAccountType(expectedAccountType)) {
            throw new BusinessException(AuthErrorCode.LOGIN_FAILED);
        }
        return accountState.snapshot();
    }

    public AuthenticatedAccount getRequiredByUserId(String userId) {
        AccountState accountState = accountsByUserId.get(userId);
        if (accountState == null) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_NOT_FOUND);
        }
        return accountState.snapshot();
    }

    public long incrementTokenVersion(String userId) {
        AccountState accountState = accountsByUserId.get(userId);
        if (accountState == null) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_NOT_FOUND);
        }
        return accountState.incrementTokenVersion();
    }

    private static final class AccountState {

        private final String userId;
        private final String accountType;
        private final String accountName;
        private final String password;
        private final String displayName;
        private final String merchantId;
        private final String storeId;
        private final String miniProgramId;
        private final List<String> roleCodes;
        private final AtomicLong tokenVersion;

        private AccountState(String userId,
                             String accountType,
                             String accountName,
                             String password,
                             String displayName,
                             String merchantId,
                             String storeId,
                             String miniProgramId,
                             List<String> roleCodes) {
            this.userId = userId;
            this.accountType = accountType;
            this.accountName = accountName;
            this.password = password;
            this.displayName = displayName;
            this.merchantId = merchantId;
            this.storeId = storeId;
            this.miniProgramId = miniProgramId;
            this.roleCodes = List.copyOf(roleCodes);
            this.tokenVersion = new AtomicLong(0L);
        }

        static AccountState from(AuthServiceProperties.BootstrapAccount bootstrapAccount) {
            if (!StringUtils.hasText(bootstrapAccount.getUserId())
                    || !StringUtils.hasText(bootstrapAccount.getAccountType())
                    || !StringUtils.hasText(bootstrapAccount.getAccountName())
                    || !StringUtils.hasText(bootstrapAccount.getPassword())) {
                throw new IllegalStateException("bootstrap-accounts 配置不完整");
            }
            return new AccountState(
                    bootstrapAccount.getUserId(),
                    normalize(bootstrapAccount.getAccountType()),
                    bootstrapAccount.getAccountName(),
                    bootstrapAccount.getPassword(),
                    bootstrapAccount.getDisplayName(),
                    bootstrapAccount.getMerchantId(),
                    bootstrapAccount.getStoreId(),
                    bootstrapAccount.getMiniProgramId(),
                    bootstrapAccount.getRoleCodes() == null ? List.of() : bootstrapAccount.getRoleCodes());
        }

        String userId() {
            return userId;
        }

        String accountName() {
            return accountName;
        }

        boolean matchesPassword(String rawPassword) {
            return password.equals(rawPassword);
        }

        boolean matchesAccountType(String expectedAccountType) {
            return accountType.equals(normalize(expectedAccountType));
        }

        long incrementTokenVersion() {
            return tokenVersion.incrementAndGet();
        }

        AuthenticatedAccount snapshot() {
            return new AuthenticatedAccount(
                    userId,
                    accountType,
                    accountName,
                    displayName,
                    merchantId,
                    storeId,
                    miniProgramId,
                    roleCodes,
                    tokenVersion.get());
        }

        private static String normalize(String raw) {
            return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        }
    }
}
