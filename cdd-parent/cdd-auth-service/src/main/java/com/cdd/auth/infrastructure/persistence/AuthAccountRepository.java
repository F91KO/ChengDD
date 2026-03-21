package com.cdd.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

public interface AuthAccountRepository {

    Optional<StoredAccount> findByAccountName(String accountName);

    Optional<StoredAccount> findByUserId(String userId);

    void updateLastLoginAt(long accountId, Instant lastLoginAt);

    long incrementTokenVersion(String userId);
}
