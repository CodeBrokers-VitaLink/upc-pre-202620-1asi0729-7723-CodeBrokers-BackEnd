package com.codebrokers.vitalink.shared.security;

import com.codebrokers.vitalink.iam.domain.UserRole;
import java.util.Objects;

public record AccountIdentity(String accountId, UserRole role) {
    public AccountIdentity {
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("Account ID is required");
        Objects.requireNonNull(role);
    }
}
