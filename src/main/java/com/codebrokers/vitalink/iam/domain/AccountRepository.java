package com.codebrokers.vitalink.iam.domain;

import java.util.Optional;

public interface AccountRepository {
    Optional<UserAccount> findById(String id);
    Optional<UserAccount> findByEmail(String normalizedEmail);
    void insert(UserAccount account);
}
