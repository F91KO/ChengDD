package com.cdd.auth.service;

import com.cdd.auth.infrastructure.persistence.AuthAccountRepository;
import com.cdd.auth.infrastructure.persistence.StoredAccount;
import com.cdd.auth.error.AuthErrorCode;
import com.cdd.common.core.error.BusinessException;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RuntimeAccountService {

    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public RuntimeAccountService(AuthAccountRepository authAccountRepository,
                                 PasswordEncoder passwordEncoder) {
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticatedAccount authenticate(String expectedAccountType, String accountName, String password) {
        StoredAccount account = authAccountRepository.findByAccountName(accountName)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.LOGIN_FAILED));
        if (!matchesAccountType(account.accountType(), expectedAccountType)
                || account.passwordHash() == null
                || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new BusinessException(AuthErrorCode.LOGIN_FAILED);
        }
        authAccountRepository.updateLastLoginAt(account.accountId(), java.time.Instant.now());
        return account.toAuthenticatedAccount();
    }

    public AuthenticatedAccount getRequiredByUserId(String userId) {
        return authAccountRepository.findByUserId(userId)
                .map(StoredAccount::toAuthenticatedAccount)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ACCOUNT_NOT_FOUND));
    }

    public long incrementTokenVersion(String userId) {
        long tokenVersion = authAccountRepository.incrementTokenVersion(userId);
        if (tokenVersion < 0) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_NOT_FOUND);
        }
        return tokenVersion;
    }

    private boolean matchesAccountType(String actualAccountType, String expectedAccountType) {
        return normalize(actualAccountType).equals(normalize(expectedAccountType));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
