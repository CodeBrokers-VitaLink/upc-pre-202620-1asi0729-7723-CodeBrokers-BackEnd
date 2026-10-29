package com.codebrokers.vitalink.iam.interfaces.rest;

import com.codebrokers.vitalink.iam.domain.UserAccount;
import com.codebrokers.vitalink.shared.security.CareContext;

public record UserResponse(String id, String name, String email, String role, String phone,
                           String hospital, CareContext careContext) {
    public static UserResponse from(UserAccount account, CareContext context) {
        return new UserResponse(account.id(), account.name(), account.email(), account.role().apiValue(), account.phone(), null, context);
    }
}
